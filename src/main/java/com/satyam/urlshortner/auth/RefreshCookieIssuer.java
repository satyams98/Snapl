package com.satyam.urlshortner.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieIssuer {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    public ResponseEntity<AuthResponse> issue(AuthResult result, ServerHttpResponse response) {
        response.addCookie(buildCookie(result.rawRefreshToken(), result.refreshTokenTtl()));
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.response());
    }

    public void clear(ServerHttpResponse response) {
        response.addCookie(buildCookie("", Duration.ZERO));
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
