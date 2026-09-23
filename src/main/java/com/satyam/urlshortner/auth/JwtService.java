package com.satyam.urlshortner.auth;

import com.satyam.urlshortner.org.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_ORG_ID = "orgId";
    private static final String CLAIM_ROLE = "role";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(AuthPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(principal.userId()))
                .claim("email", principal.email())
                .claim(CLAIM_ORG_ID, principal.orgId())
                .claim(CLAIM_ROLE, principal.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl())))
                .signWith(signingKey)
                .compact();
    }

    public AuthPrincipal parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get(CLAIM_ORG_ID, Long.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Access token is invalid or expired");
        }
    }

    // Refresh tokens are opaque random values, not JWTs; only their SHA-256 hash is ever persisted.
    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Duration accessTokenTtl() {
        return Duration.ofMinutes(properties.accessTokenTtlMinutes());
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(properties.refreshTokenTtlDays());
    }
}
