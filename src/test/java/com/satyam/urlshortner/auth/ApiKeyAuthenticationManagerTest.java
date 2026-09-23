package com.satyam.urlshortner.auth;

import com.satyam.urlshortner.apikey.ApiKey;
import com.satyam.urlshortner.apikey.ApiKeyRepository;
import com.satyam.urlshortner.url.UrlHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationManagerTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyAuthenticationManager manager;

    @BeforeEach
    void setUp() {
        manager = new ApiKeyAuthenticationManager(apiKeyRepository);
    }

    @Test
    void authenticatesValidKeyAndTouchesLastUsed() {
        ApiKey key = new ApiKey(1L, 10L, "CI key", "usk_abc123", UrlHasher.sha256Hex("usk_raw-value"), "READ,WRITE",
                Instant.now(), null, null);
        when(apiKeyRepository.findByHashedSecretAndRevokedAtIsNull(UrlHasher.sha256Hex("usk_raw-value")))
                .thenReturn(Mono.just(key));
        when(apiKeyRepository.touchLastUsed(eq(1L), any(Instant.class))).thenReturn(Mono.just(1));

        StepVerifier.create(manager.authenticate(new UsernamePasswordAuthenticationToken("usk_raw-value", "usk_raw-value")))
                .assertNext(auth -> {
                    AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
                    assertEquals(10L, principal.orgId());
                    assertEquals(2, principal.apiKeyScopes().size());
                })
                .verifyComplete();

        verify(apiKeyRepository).touchLastUsed(eq(1L), any(Instant.class));
    }

    @Test
    void rejectsUnknownOrRevokedKey() {
        when(apiKeyRepository.findByHashedSecretAndRevokedAtIsNull(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(manager.authenticate(new UsernamePasswordAuthenticationToken("bad-key", "bad-key")))
                .expectError(BadCredentialsException.class)
                .verify();
    }
}
