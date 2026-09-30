package com.satyam.urlshortner.org;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MembershipRepository extends ReactiveCrudRepository<Membership, Long> {
    // A user's "current" org for login/refresh is the org they joined most recently — this makes
    // accepting an invitation into a new org actually take effect on the next login/refresh.
    Mono<Membership> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Mono<Membership> findByUserIdAndOrgId(Long userId, Long orgId);

    Flux<Membership> findByOrgId(Long orgId);
}
