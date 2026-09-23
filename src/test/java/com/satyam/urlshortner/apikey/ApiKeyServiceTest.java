package com.satyam.urlshortner.apikey;

import com.satyam.urlshortner.auth.AuthPrincipal;
import com.satyam.urlshortner.org.AccessDeniedForRoleException;
import com.satyam.urlshortner.org.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService service;

    private static final AuthPrincipal ADMIN = AuthPrincipal.forUser(1L, "admin@acme.test", 10L, Role.ADMIN);
    private static final AuthPrincipal MEMBER = AuthPrincipal.forUser(2L, "member@acme.test", 10L, Role.MEMBER);

    @BeforeEach
    void setUp() {
        service = new ApiKeyService(apiKeyRepository);
    }

    @Test
    void createRejectedForMemberRole() {
        StepVerifier.create(service.create(MEMBER, new CreateApiKeyRequest("CI key", List.of(ApiKeyScope.READ))))
                .expectError(AccessDeniedForRoleException.class)
                .verify();

        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void createSucceedsForAdminAndReturnsPlaintextKeyOnce() {
        when(apiKeyRepository.save(any())).thenAnswer(inv -> {
            ApiKey saved = inv.getArgument(0);
            saved.setId(99L);
            return Mono.just(saved);
        });

        StepVerifier.create(service.create(ADMIN, new CreateApiKeyRequest("CI key", List.of(ApiKeyScope.READ, ApiKeyScope.WRITE))))
                .assertNext(response -> {
                    assertEquals("CI key", response.name());
                    assertTrue(response.apiKey().startsWith("usk_"));
                    assertEquals(List.of("READ", "WRITE"), response.scopes());
                })
                .verifyComplete();
    }

    @Test
    void revokeRejectedForMemberRole() {
        StepVerifier.create(service.revoke(MEMBER, 5L))
                .expectError(AccessDeniedForRoleException.class)
                .verify();

        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void revokeFailsWhenKeyNotFoundInOrg() {
        when(apiKeyRepository.revoke(eq(10L), eq(5L), any(Instant.class))).thenReturn(Mono.just(0));

        StepVerifier.create(service.revoke(ADMIN, 5L))
                .expectError(ApiKeyNotFoundException.class)
                .verify();
    }

    @Test
    void revokeSucceedsForAdmin() {
        when(apiKeyRepository.revoke(eq(10L), eq(5L), any(Instant.class))).thenReturn(Mono.just(1));

        StepVerifier.create(service.revoke(ADMIN, 5L)).verifyComplete();
    }
}
