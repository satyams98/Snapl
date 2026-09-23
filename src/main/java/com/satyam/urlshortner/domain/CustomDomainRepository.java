package com.satyam.urlshortner.domain;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface CustomDomainRepository extends ReactiveCrudRepository<CustomDomain, Long> {
    Flux<CustomDomain> findByOrgIdOrderByCreatedAtDesc(Long orgId);

    Mono<CustomDomain> findByOrgIdAndId(Long orgId, Long id);

    Mono<Boolean> existsByDomain(String domain);

    Mono<Long> countByOrgId(Long orgId);

    // Only verified domains may resolve redirects on behalf of an org.
    Mono<CustomDomain> findByDomainAndVerifiedAtIsNotNull(String domain);

    @Modifying
    @Query("UPDATE custom_domains SET verified_at = :verifiedAt WHERE id = :id")
    Mono<Integer> markVerified(Long id, Instant verifiedAt);
}
