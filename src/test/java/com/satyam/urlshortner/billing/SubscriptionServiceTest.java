package com.satyam.urlshortner.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private PlanRepository planRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(planRepository, subscriptionRepository);
    }

    @Test
    void createDefaultSubscriptionUsesFreePlan() {
        Plan freePlan = new Plan(1L, "FREE", "Free", 0, 25, 0, 1000, false, null);
        when(planRepository.findByCode("FREE")).thenReturn(Mono.just(freePlan));
        when(subscriptionRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.createDefaultSubscription(42L))
                .assertNext(sub -> {
                    org.junit.jupiter.api.Assertions.assertEquals(42L, sub.getOrgId());
                    org.junit.jupiter.api.Assertions.assertEquals(1L, sub.getPlanId());
                    org.junit.jupiter.api.Assertions.assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void createDefaultSubscriptionFailsWhenFreePlanNotSeeded() {
        when(planRepository.findByCode("FREE")).thenReturn(Mono.empty());

        StepVerifier.create(service.createDefaultSubscription(42L))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void getCurrentPlanFailsWhenNoSubscriptionExists() {
        when(subscriptionRepository.findByOrgId(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.getCurrentPlan(99L))
                .expectError(NoSubscriptionException.class)
                .verify();
    }
}
