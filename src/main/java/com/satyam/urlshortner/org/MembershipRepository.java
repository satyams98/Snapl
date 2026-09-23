package com.satyam.urlshortner.org;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MembershipRepository extends ReactiveCrudRepository<Membership, Long> {
    // A user's "current" org for login/refresh is the org they joined first (Phase 1 supports one active org per session).
    Mono<Membership> findFirstByUserIdOrderByCreatedAtAsc(Long userId);

    Mono<Membership> findByUserIdAndOrgId(Long userId, Long orgId);

    Flux<Membership> findByOrgId(Long orgId);
}
