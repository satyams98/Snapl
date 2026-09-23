package com.satyam.urlshortner.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String DEFAULT_PLAN_CODE = "FREE";

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public Mono<Subscription> createDefaultSubscription(Long orgId) {
        return planRepository.findByCode(DEFAULT_PLAN_CODE)
                .switchIfEmpty(Mono.error(new IllegalStateException("Default plan '" + DEFAULT_PLAN_CODE + "' is not seeded")))
                .flatMap(plan -> subscriptionRepository.save(new Subscription(null, orgId, plan.getId(),
                        SubscriptionStatus.ACTIVE, null, null, null, Instant.now(), Instant.now())));
    }

    public Mono<Subscription> getSubscription(Long orgId) {
        return subscriptionRepository.findByOrgId(orgId)
                .switchIfEmpty(Mono.error(new NoSubscriptionException(orgId)));
    }

    public Mono<Plan> getCurrentPlan(Long orgId) {
        return getSubscription(orgId).flatMap(subscription -> planRepository.findById(subscription.getPlanId()));
    }
}
