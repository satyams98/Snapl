package com.satyam.urlshortner.auth;

// Only the access token travels in the response body; the refresh token is set as an HttpOnly cookie.
public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        Long userId,
        String email,
        Long orgId,
        String organizationName,
        String role
) {}
