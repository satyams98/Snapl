package com.satyam.urlshortner.url;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TagRepository extends ReactiveCrudRepository<Tag, Long> {
    Flux<Tag> findByOrgIdOrderByNameAsc(Long orgId);

    Mono<Tag> findByOrgIdAndName(Long orgId, String name);
}
