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
class StripeCheckoutServiceTest {

    @Mock
    private PlanRepository planRepository;

    private StripeCheckoutService service;

    @BeforeEach
    void setUp() {
        service = new StripeCheckoutService(new StripeProperties("", "", "http://success", "http://cancel"), planRepository);
    }

    @Test
    void createCheckoutSessionFailsWhenStripeNotConfigured() {
        StepVerifier.create(service.createCheckoutSessionUrl(1L, "PRO"))
                .expectError(StripeNotConfiguredException.class)
                .verify();
    }

    @Test
    void createCheckoutSessionFailsWhenPlanNotFound() {
        StripeCheckoutService configured = new StripeCheckoutService(
                new StripeProperties("sk_test_dummy", "", "http://success", "http://cancel"), planRepository);
        when(planRepository.findByCode("MISSING")).thenReturn(Mono.empty());

        StepVerifier.create(configured.createCheckoutSessionUrl(1L, "MISSING"))
                .expectError(PlanNotFoundException.class)
                .verify();
    }

    @Test
    void createCheckoutSessionFailsWhenPlanHasNoStripePrice() {
        StripeCheckoutService configured = new StripeCheckoutService(
                new StripeProperties("sk_test_dummy", "", "http://success", "http://cancel"), planRepository);
        Plan planWithoutPrice = new Plan(2L, "PRO", "Pro", 2900, 1000, 1, 50000, true, null);
        when(planRepository.findByCode("PRO")).thenReturn(Mono.just(planWithoutPrice));

        StepVerifier.create(configured.createCheckoutSessionUrl(1L, "PRO"))
                .expectError(StripePriceNotConfiguredException.class)
                .verify();
    }
}
