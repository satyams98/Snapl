package com.satyam.urlshortner.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, ServerHttpResponse response) {
        return authService.register(request).map(result -> withRefreshCookie(result, response));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request, ServerHttpResponse response) {
        return authService.login(request.email(), request.password()).map(result -> withRefreshCookie(result, response));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@CookieValue(REFRESH_COOKIE_NAME) String refreshToken, ServerHttpResponse response) {
        return authService.refresh(refreshToken).map(result -> withRefreshCookie(result, response));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
                                              ServerHttpResponse response) {
        Mono<Void> revoke = refreshToken != null ? authService.logout(refreshToken) : Mono.empty();
        return revoke.then(Mono.fromSupplier(() -> {
            response.addCookie(clearedCookie());
            return ResponseEntity.noContent().<Void>build();
        }));
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResult result, ServerHttpResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, result.rawRefreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(result.refreshTokenTtl())
                .build();
        response.addCookie(cookie);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.response());
    }

    private ResponseCookie clearedCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
