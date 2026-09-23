package com.satyam.urlshortner.org;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OrganizationRepository extends ReactiveCrudRepository<Organization, Long> {
    Mono<Boolean> existsBySlug(String slug);
}
