package com.satyam.urlshortner.auth;

import com.satyam.urlshortner.apikey.ApiKey;
import com.satyam.urlshortner.apikey.ApiKeyRepository;
import com.satyam.urlshortner.apikey.ApiKeyScope;
import com.satyam.urlshortner.url.UrlHasher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ApiKeyAuthenticationManager implements ReactiveAuthenticationManager {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationManager(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String rawKey = String.valueOf(authentication.getCredentials());
        String hash = UrlHasher.sha256Hex(rawKey);

        return apiKeyRepository.findByHashedSecretAndRevokedAtIsNull(hash)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid or revoked API key")))
                .doOnNext(key -> apiKeyRepository.touchLastUsed(key.getId(), Instant.now()).subscribe())
                .map(this::toAuthentication);
    }

    private Authentication toAuthentication(ApiKey key) {
        Set<ApiKeyScope> scopes = parseScopes(key.getScopes());
        AuthPrincipal principal = AuthPrincipal.forApiKey(key.getOrgId(), scopes);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
    }

    private Set<ApiKeyScope> parseScopes(String scopes) {
        return Arrays.stream(scopes.split(",")).map(ApiKeyScope::valueOf).collect(Collectors.toSet());
    }
}
