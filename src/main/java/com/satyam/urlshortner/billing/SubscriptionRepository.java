package com.satyam.urlshortner.billing;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface SubscriptionRepository extends ReactiveCrudRepository<Subscription, Long> {
    Mono<Subscription> findByOrgId(Long orgId);

    Mono<Subscription> findByStripeCustomerId(String stripeCustomerId);

    @Modifying
    @Query("""
            UPDATE subscriptions SET plan_id = :planId, status = :status, stripe_customer_id = :stripeCustomerId,
                stripe_subscription_id = :stripeSubscriptionId, current_period_end = :currentPeriodEnd, updated_at = :updatedAt
            WHERE org_id = :orgId
            """)
    Mono<Integer> updateForOrg(Long orgId, Long planId, SubscriptionStatus status, String stripeCustomerId,
                                String stripeSubscriptionId, Instant currentPeriodEnd, Instant updatedAt);
}
