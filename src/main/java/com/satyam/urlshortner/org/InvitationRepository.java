package com.satyam.urlshortner.org;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface InvitationRepository extends ReactiveCrudRepository<Invitation, Long> {
    Mono<Invitation> findByToken(String token);

    Flux<Invitation> findByOrgIdAndAcceptedAtIsNull(Long orgId);

    @Modifying
    @Query("UPDATE invitations SET accepted_at = :acceptedAt WHERE token = :token AND accepted_at IS NULL")
    Mono<Integer> acceptByToken(String token, Instant acceptedAt);
}
