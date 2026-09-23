package com.satyam.urlshortner.auth;

import com.satyam.urlshortner.org.Membership;
import com.satyam.urlshortner.org.MembershipRepository;
import com.satyam.urlshortner.org.Organization;
import com.satyam.urlshortner.org.OrganizationRepository;
import com.satyam.urlshortner.org.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(new JwtProperties("unit-test-signing-secret-32-bytes-min!!", 15, 30));
        authService = new AuthService(userRepository, organizationRepository, membershipRepository,
                refreshTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesOrgUserMembershipAndTokens() {
        when(userRepository.existsByEmail("owner@acme.test")).thenReturn(Mono.just(false));
        when(organizationRepository.existsBySlug("acme")).thenReturn(Mono.just(false));
        when(organizationRepository.save(any())).thenAnswer(inv -> Mono.just(withId(inv.getArgument(0), 10L)));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> Mono.just(withId(inv.getArgument(0), 20L)));
        when(membershipRepository.save(any())).thenAnswer(inv -> Mono.just(withId(inv.getArgument(0), 30L)));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(authService.register(new RegisterRequest("owner@acme.test", "password123", "Owner", "Acme")))
                .assertNext(result -> {
                    assertEquals("Acme", result.response().organizationName());
                    assertEquals("OWNER", result.response().role());
                    assertEquals(20L, result.response().userId());
                    assertEquals(10L, result.response().orgId());
                })
                .verifyComplete();

        verify(membershipRepository).save(argThat(m -> m.getRole() == Role.OWNER));
    }

    @Test
    void registerFailsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("owner@acme.test")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(new RegisterRequest("owner@acme.test", "password123", "Owner", "Acme")))
                .expectError(EmailAlreadyRegisteredException.class)
                .verify();

        verifyNoInteractions(organizationRepository);
    }

    @Test
    void loginFailsForUnknownEmail() {
        when(userRepository.findByEmail("missing@acme.test")).thenReturn(Mono.empty());

        StepVerifier.create(authService.login("missing@acme.test", "password123"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void loginFailsForWrongPassword() {
        User user = new User(1L, "owner@acme.test", "hashed", "Owner", Instant.now());
        when(userRepository.findByEmail("owner@acme.test")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        StepVerifier.create(authService.login("owner@acme.test", "wrong"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void loginFailsWhenUserHasNoMembership() {
        User user = new User(1L, "owner@acme.test", "hashed", "Owner", Instant.now());
        when(userRepository.findByEmail("owner@acme.test")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(membershipRepository.findFirstByUserIdOrderByCreatedAtAsc(1L)).thenReturn(Mono.empty());

        StepVerifier.create(authService.login("owner@acme.test", "password123"))
                .expectError(NoOrganizationMembershipException.class)
                .verify();
    }

    @Test
    void loginSucceedsAndIssuesTokens() {
        User user = new User(1L, "owner@acme.test", "hashed", "Owner", Instant.now());
        Membership membership = new Membership(5L, 1L, 10L, Role.ADMIN, Instant.now());
        Organization org = new Organization(10L, "Acme", "acme", Instant.now());

        when(userRepository.findByEmail("owner@acme.test")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(membershipRepository.findFirstByUserIdOrderByCreatedAtAsc(1L)).thenReturn(Mono.just(membership));
        when(organizationRepository.findById(10L)).thenReturn(Mono.just(org));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(authService.login("owner@acme.test", "password123"))
                .assertNext(result -> {
                    assertEquals("ADMIN", result.response().role());
                    assertEquals(10L, result.response().orgId());
                })
                .verifyComplete();
    }

    @Test
    void refreshRejectsExpiredToken() {
        RefreshToken expired = new RefreshToken(1L, 1L, anyHash(), Instant.now().minusSeconds(10), null, Instant.now());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Mono.just(expired));

        StepVerifier.create(authService.refresh("raw-token"))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    void refreshRejectsRevokedToken() {
        RefreshToken revoked = new RefreshToken(1L, 1L, anyHash(), Instant.now().plusSeconds(3600), Instant.now(), Instant.now());
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Mono.just(revoked));

        StepVerifier.create(authService.refresh("raw-token"))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(authService.refresh("raw-token"))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    private String anyHash() {
        return "0".repeat(64);
    }

    private <T> T withId(Object entity, Long id) {
        if (entity instanceof Organization org) {
            return (T) new Organization(id, org.getName(), org.getSlug(), org.getCreatedAt());
        }
        if (entity instanceof User user) {
            return (T) new User(id, user.getEmail(), user.getPasswordHash(), user.getName(), user.getCreatedAt());
        }
        if (entity instanceof Membership membership) {
            return (T) new Membership(id, membership.getUserId(), membership.getOrgId(), membership.getRole(), membership.getCreatedAt());
        }
        throw new IllegalArgumentException("Unsupported entity: " + entity);
    }
}
