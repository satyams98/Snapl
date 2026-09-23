package com.satyam.urlshortner.auth;

import java.time.Duration;

// Carries the raw (unhashed) refresh token value out of the service layer just long enough
// for the controller to set it as an HttpOnly cookie; it is never returned in a JSON body.
public record AuthResult(AuthResponse response, String rawRefreshToken, Duration refreshTokenTtl) {
}
