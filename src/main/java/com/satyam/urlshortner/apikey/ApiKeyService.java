package com.satyam.urlshortner.apikey;

import com.satyam.urlshortner.auth.AuthPrincipal;
import com.satyam.urlshortner.billing.PlanLimitEnforcer;
import com.satyam.urlshortner.org.AccessDeniedForRoleException;
import com.satyam.urlshortner.org.Role;
import com.satyam.urlshortner.url.UrlHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final Set<Role> CAN_MANAGE_KEYS = EnumSet.of(Role.OWNER, Role.ADMIN);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "usk_";

    private final ApiKeyRepository apiKeyRepository;
    private final PlanLimitEnforcer planLimitEnforcer;

    public Flux<ApiKeyResponse> list(AuthPrincipal principal) {
        return apiKeyRepository.findByOrgIdOrderByCreatedAtDesc(principal.orgId()).map(ApiKeyResponse::from);
    }

    public Mono<ApiKeyCreatedResponse> create(AuthPrincipal principal, CreateApiKeyRequest request) {
        return requireManagerRole(principal)
                .then(Mono.defer(() -> planLimitEnforcer.checkApiAccessAllowed(principal.orgId())))
                .then(Mono.defer(() -> {
                    String rawKey = generateRawKey();
                    String scopes = request.scopes().stream().map(Enum::name).collect(Collectors.joining(","));
                    ApiKey entity = new ApiKey(null, principal.orgId(), request.name(), displayPrefix(rawKey),
                            UrlHasher.sha256Hex(rawKey), scopes, Instant.now(), null, null);
                    return apiKeyRepository.save(entity)
                            .map(saved -> new ApiKeyCreatedResponse(saved.getId(), saved.getName(), rawKey,
                                    request.scopes().stream().map(Enum::name).toList(), saved.getCreatedAt()));
                }));
    }

    public Mono<Void> revoke(AuthPrincipal principal, Long id) {
        return requireManagerRole(principal)
                .then(Mono.defer(() -> apiKeyRepository.revoke(principal.orgId(), id, Instant.now())))
                .flatMap(rows -> rows > 0 ? Mono.empty() : Mono.error(new ApiKeyNotFoundException(id)));
    }

    private Mono<Void> requireManagerRole(AuthPrincipal principal) {
        return CAN_MANAGE_KEYS.contains(principal.role())
                ? Mono.empty()
                : Mono.error(new AccessDeniedForRoleException(Role.ADMIN));
    }

    private String generateRawKey() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String displayPrefix(String rawKey) {
        return rawKey.substring(0, Math.min(rawKey.length(), KEY_PREFIX.length() + 6));
    }
}
