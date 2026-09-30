package com.satyam.urlshortner.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieIssuer refreshCookieIssuer;

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, ServerHttpResponse response) {
        return authService.register(request).map(result -> refreshCookieIssuer.issue(result, response));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request, ServerHttpResponse response) {
        return authService.login(request.email(), request.password()).map(result -> refreshCookieIssuer.issue(result, response));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@CookieValue(RefreshCookieIssuer.REFRESH_COOKIE_NAME) String refreshToken, ServerHttpResponse response) {
        return authService.refresh(refreshToken).map(result -> refreshCookieIssuer.issue(result, response));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@CookieValue(name = RefreshCookieIssuer.REFRESH_COOKIE_NAME, required = false) String refreshToken,
                                              ServerHttpResponse response) {
        Mono<Void> revoke = refreshToken != null ? authService.logout(refreshToken) : Mono.empty();
        return revoke.then(Mono.fromSupplier(() -> {
            refreshCookieIssuer.clear(response);
            return ResponseEntity.noContent().<Void>build();
        }));
    }
}
