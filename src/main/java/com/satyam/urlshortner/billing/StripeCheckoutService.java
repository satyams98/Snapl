package com.satyam.urlshortner.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

// NOTE: requires real app.stripe.secret-key / plan.stripePriceId values to actually call Stripe;
// without them, createCheckoutSessionUrl fails fast with a clear error rather than a raw Stripe exception.
@Service
@RequiredArgsConstructor
public class StripeCheckoutService {

    private final StripeProperties properties;
    private final PlanRepository planRepository;

    public Mono<String> createCheckoutSessionUrl(Long orgId, String planCode) {
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            return Mono.error(new StripeNotConfiguredException());
        }
        return planRepository.findByCode(planCode)
                .switchIfEmpty(Mono.error(new PlanNotFoundException(planCode)))
                .flatMap(plan -> {
                    if (plan.getStripePriceId() == null || plan.getStripePriceId().isBlank()) {
                        return Mono.error(new StripePriceNotConfiguredException(planCode));
                    }
                    return Mono.fromCallable(() -> buildCheckoutSession(orgId, plan)).subscribeOn(Schedulers.boundedElastic());
                });
    }

    private String buildCheckoutSession(Long orgId, Plan plan) throws StripeException {
        Stripe.apiKey = properties.secretKey();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(properties.successUrl())
                .setCancelUrl(properties.cancelUrl())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(plan.getStripePriceId())
                        .setQuantity(1L)
                        .build())
                .putMetadata("orgId", String.valueOf(orgId))
                .putMetadata("planCode", plan.getCode())
                .build();
        return Session.create(params).getUrl();
    }
}
