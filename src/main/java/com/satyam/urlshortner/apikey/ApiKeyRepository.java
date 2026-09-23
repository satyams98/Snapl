package com.satyam.urlshortner.apikey;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface ApiKeyRepository extends ReactiveCrudRepository<ApiKey, Long> {
    Flux<ApiKey> findByOrgIdOrderByCreatedAtDesc(Long orgId);

    Mono<ApiKey> findByHashedSecretAndRevokedAtIsNull(String hashedSecret);

    @Modifying
    @Query("UPDATE api_keys SET revoked_at = :revokedAt WHERE org_id = :orgId AND id = :id AND revoked_at IS NULL")
    Mono<Integer> revoke(Long orgId, Long id, Instant revokedAt);

    @Modifying
    @Query("UPDATE api_keys SET last_used_at = :usedAt WHERE id = :id")
    Mono<Integer> touchLastUsed(Long id, Instant usedAt);
}
