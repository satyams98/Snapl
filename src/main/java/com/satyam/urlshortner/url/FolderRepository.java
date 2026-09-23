package com.satyam.urlshortner.url;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FolderRepository extends ReactiveCrudRepository<Folder, Long> {
    Flux<Folder> findByOrgIdOrderByNameAsc(Long orgId);

    Mono<Folder> findByOrgIdAndId(Long orgId, Long id);

    Mono<Folder> findByOrgIdAndName(Long orgId, String name);
}
