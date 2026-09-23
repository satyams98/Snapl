package com.satyam.urlshortner.auth;

import com.satyam.urlshortner.org.Membership;
import com.satyam.urlshortner.org.MembershipRepository;
import com.satyam.urlshortner.org.Organization;
import com.satyam.urlshortner.org.OrganizationRepository;
import com.satyam.urlshortner.org.Role;
import com.satyam.urlshortner.url.UrlHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Mono<AuthResult> register(RegisterRequest request) {
        return userRepository.existsByEmail(request.email())
                .flatMap(exists -> exists
                        ? Mono.error(new EmailAlreadyRegisteredException(request.email()))
                        : createOrganization(request.organizationName()))
                .flatMap(org -> createUser(request).flatMap(user -> createMembership(user, org, Role.OWNER)
                        .flatMap(membership -> issueTokens(user, membership, org))));
    }

    public Mono<AuthResult> login(String email, String password) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                .flatMap(user -> passwordEncoder.matches(password, user.getPasswordHash())
                        ? Mono.just(user)
                        : Mono.error(new InvalidCredentialsException()))
                .flatMap(this::issueTokensForCurrentOrg);
    }

    public Mono<AuthResult> refresh(String rawRefreshToken) {
        String hash = UrlHasher.sha256Hex(rawRefreshToken);
        return refreshTokenRepository.findByTokenHash(hash)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Refresh token not recognized")))
                .flatMap(this::validateActive)
                .flatMap(token -> refreshTokenRepository.revokeByTokenHash(hash, Instant.now())
                        .then(userRepository.findById(token.getUserId())))
                .switchIfEmpty(Mono.error(new InvalidTokenException("Refresh token owner no longer exists")))
                .flatMap(this::issueTokensForCurrentOrg);
    }

    public Mono<Void> logout(String rawRefreshToken) {
        String hash = UrlHasher.sha256Hex(rawRefreshToken);
        return refreshTokenRepository.revokeByTokenHash(hash, Instant.now()).then();
    }

    private Mono<RefreshToken> validateActive(RefreshToken token) {
        boolean expired = token.getExpiresAt().isBefore(Instant.now());
        boolean revoked = token.getRevokedAt() != null;
        return (expired || revoked)
                ? Mono.error(new InvalidTokenException("Refresh token expired or already used"))
                : Mono.just(token);
    }

    private Mono<AuthResult> issueTokensForCurrentOrg(User user) {
        return membershipRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
                .switchIfEmpty(Mono.error(new NoOrganizationMembershipException(user.getId())))
                .flatMap(membership -> organizationRepository.findById(membership.getOrgId())
                        .flatMap(org -> issueTokens(user, membership, org)));
    }

    private Mono<AuthResult> issueTokens(User user, Membership membership, Organization org) {
        AuthPrincipal principal = AuthPrincipal.forUser(user.getId(), user.getEmail(), org.getId(), membership.getRole());
        String accessToken = jwtService.issueAccessToken(principal);
        String rawRefreshToken = jwtService.generateRefreshTokenValue();
        RefreshToken refreshToken = new RefreshToken(null, user.getId(), UrlHasher.sha256Hex(rawRefreshToken),
                Instant.now().plus(jwtService.refreshTokenTtl()), null, Instant.now());

        return refreshTokenRepository.save(refreshToken).map(saved -> new AuthResult(
                new AuthResponse(accessToken, jwtService.accessTokenTtl().toSeconds(), user.getId(), user.getEmail(),
                        org.getId(), org.getName(), membership.getRole().name()),
                rawRefreshToken,
                jwtService.refreshTokenTtl()));
    }

    private Mono<Organization> createOrganization(String name) {
        String baseSlug = slugify(name);
        return organizationRepository.existsBySlug(baseSlug)
                .flatMap(taken -> {
                    String slug = taken ? baseSlug + "-" + System.currentTimeMillis() : baseSlug;
                    return organizationRepository.save(new Organization(null, name, slug, Instant.now()));
                });
    }

    private Mono<User> createUser(RegisterRequest request) {
        User user = new User(null, request.email(), passwordEncoder.encode(request.password()), request.name(), Instant.now());
        return userRepository.save(user);
    }

    private Mono<Membership> createMembership(User user, Organization org, Role role) {
        return membershipRepository.save(new Membership(null, user.getId(), org.getId(), role, Instant.now()));
    }

    private String slugify(String name) {
        String lowered = name.toLowerCase(Locale.ROOT).trim();
        String slug = NON_SLUG_CHARS.matcher(lowered).replaceAll("-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "org" : slug;
    }
}
