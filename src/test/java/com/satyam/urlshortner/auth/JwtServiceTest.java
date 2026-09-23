package com.satyam.urlshortner.auth;

import com.satyam.urlshortner.org.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties("test-signing-secret-at-least-32-bytes-long!", 15, 30));
    }

    @Test
    void issuedAccessTokenRoundTripsToSamePrincipal() {
        AuthPrincipal principal = new AuthPrincipal(42L, "user@acme.test", 7L, Role.ADMIN);

        String token = jwtService.issueAccessToken(principal);
        AuthPrincipal parsed = jwtService.parseAccessToken(token);

        assertEquals(principal, parsed);
    }

    @Test
    void parseAccessTokenRejectsGarbageToken() {
        assertThrows(InvalidTokenException.class, () -> jwtService.parseAccessToken("not-a-jwt"));
    }

    @Test
    void parseAccessTokenRejectsTokenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService(new JwtProperties("a-completely-different-secret-32-bytes+", 15, 30));
        String token = otherService.issueAccessToken(new AuthPrincipal(1L, "a@b.test", 1L, Role.OWNER));

        assertThrows(InvalidTokenException.class, () -> jwtService.parseAccessToken(token));
    }

    @Test
    void generateRefreshTokenValueProducesNonNullUniqueValues() {
        String first = jwtService.generateRefreshTokenValue();
        String second = jwtService.generateRefreshTokenValue();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEqualsIgnoreCase(first, second);
    }

    private void assertNotEqualsIgnoreCase(String first, String second) {
        if (first.equals(second)) {
            throw new AssertionError("Expected distinct refresh token values but both were: " + first);
        }
    }
}
