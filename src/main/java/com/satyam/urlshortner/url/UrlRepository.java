package com.satyam.urlshortner.url;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface UrlRepository extends ReactiveCrudRepository<UrlEntity, Long> {
    Mono<UrlEntity> findByShortCode(String shortCode);

    Mono<UrlEntity> findFirstByLongUrlHash(String longUrlHash);

    // Direct update avoids the insert-vs-update ambiguity from UrlEntity's Persistable.isNew()==true override.
    @Modifying
    @Query("UPDATE urls SET disabled_at = :disabledAt WHERE short_code = :shortCode AND disabled_at IS NULL")
    Mono<Integer> disableByShortCode(String shortCode, Instant disabledAt);
}