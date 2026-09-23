package com.satyam.urlshortner.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PlanRepository planRepository;

    private StripeWebhookService service;

    @BeforeEach
    void setUp() {
        service = new StripeWebhookService(new StripeProperties("", "", "", ""), subscriptionRepository, planRepository);
    }

    @Test
    void handleWebhookFailsWhenNotConfigured() {
        StepVerifier.create(service.handleWebhook("{}", "sig"))
                .expectError(StripeNotConfiguredException.class)
                .verify();
    }

    @Test
    void applySubscriptionUpdatesTheOrgsSubscriptionRow() {
        when(subscriptionRepository.updateForOrg(eq(1L), eq(2L), eq(SubscriptionStatus.ACTIVE), eq("cus_123"),
                eq("sub_123"), any(), any(Instant.class))).thenReturn(Mono.just(1));

        StepVerifier.create(service.applySubscription(1L, 2L, SubscriptionStatus.ACTIVE, "cus_123", "sub_123", null))
                .verifyComplete();
    }
}
