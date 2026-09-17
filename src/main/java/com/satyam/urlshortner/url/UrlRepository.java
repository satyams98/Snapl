package com.satyam.urlshortner.url;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UrlRepository extends ReactiveCrudRepository<UrlEntity, Long> {
    Mono<UrlEntity> findByShortCode(String shortCode);

    Mono<UrlEntity> findFirstByLongUrlHash(String longUrlHash);
}