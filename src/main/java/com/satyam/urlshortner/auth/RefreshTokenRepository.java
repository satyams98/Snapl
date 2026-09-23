package com.satyam.urlshortner.auth;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {
    Mono<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE refresh_tokens SET revoked_at = :revokedAt WHERE token_hash = :tokenHash AND revoked_at IS NULL")
    Mono<Integer> revokeByTokenHash(String tokenHash, Instant revokedAt);
}
