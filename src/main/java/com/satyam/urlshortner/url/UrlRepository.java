package com.satyam.urlshortner.url;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface UrlRepository extends ReactiveCrudRepository<UrlEntity, Long> {
    Mono<UrlEntity> findByShortCode(String shortCode);

    Mono<UrlEntity> findByShortCodeAndOrgId(String shortCode, Long orgId);

    Mono<UrlEntity> findFirstByLongUrlHash(String longUrlHash);

    Mono<UrlEntity> findFirstByLongUrlHashAndOrgId(String longUrlHash, Long orgId);

    // Direct update avoids the insert-vs-update ambiguity from UrlEntity's Persistable.isNew()==true override.
    @Modifying
    @Query("UPDATE urls SET disabled_at = :disabledAt WHERE short_code = :shortCode AND org_id = :orgId AND disabled_at IS NULL")
    Mono<Integer> disableByShortCodeAndOrgId(String shortCode, Long orgId, Instant disabledAt);

    @Modifying
    @Query("UPDATE urls SET folder_id = :folderId WHERE short_code = :shortCode AND org_id = :orgId")
    Mono<Integer> updateFolderByShortCodeAndOrgId(String shortCode, Long orgId, Long folderId);

    @Modifying
    @Query("UPDATE urls SET long_url = :longUrl, long_url_hash = :longUrlHash, expires_at = :expiresAt, folder_id = :folderId WHERE short_code = :shortCode AND org_id = :orgId")
    Mono<Integer> updateDetails(String shortCode, Long orgId, String longUrl, String longUrlHash, Instant expiresAt, Long folderId);
}