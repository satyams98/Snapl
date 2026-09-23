package com.satyam.urlshortner.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

// Reads the AuthPrincipal populated by JwtAuthenticationManager out of the reactive security context.
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Mono<AuthPrincipal> get() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(AuthPrincipal.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("No authenticated principal in context")));
    }
}
