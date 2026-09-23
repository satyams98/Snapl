package com.satyam.urlshortner.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

// NOTE: signature verification and event parsing require a real app.stripe.webhook-secret and genuine
// Stripe-signed payloads, so this path is unverifiable without a live Stripe account; applySubscription()
// (the actual DB update) is factored out separately so it can be unit tested without the Stripe SDK.
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final StripeProperties properties;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public Mono<Void> handleWebhook(String payload, String signatureHeader) {
        if (properties.webhookSecret() == null || properties.webhookSecret().isBlank()) {
            return Mono.error(new StripeNotConfiguredException());
        }
        return Mono.fromCallable(() -> Webhook.constructEvent(payload, signatureHeader, properties.webhookSecret()))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(SignatureVerificationException.class, ex -> new InvalidStripeWebhookSignatureException())
                .flatMap(this::dispatch);
    }

    private Mono<Void> dispatch(Event event) {
        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        return switch (event.getType()) {
            case "checkout.session.completed" -> stripeObject instanceof Session session
                    ? applyCheckoutCompleted(session) : Mono.empty();
            case "customer.subscription.updated" -> stripeObject instanceof com.stripe.model.Subscription sub
                    ? applySubscriptionUpdated(sub) : Mono.empty();
            case "customer.subscription.deleted" -> stripeObject instanceof com.stripe.model.Subscription sub
                    ? applySubscriptionCanceled(sub) : Mono.empty();
            default -> Mono.empty();
        };
    }

    private Mono<Void> applyCheckoutCompleted(Session session) {
        if (session.getMetadata() == null) {
            return Mono.empty();
        }
        String orgIdRaw = session.getMetadata().get("orgId");
        String planCode = session.getMetadata().get("planCode");
        if (orgIdRaw == null || planCode == null) {
            return Mono.empty();
        }
        return planRepository.findByCode(planCode)
                .flatMap(plan -> applySubscription(Long.valueOf(orgIdRaw), plan.getId(), SubscriptionStatus.ACTIVE,
                        session.getCustomer(), session.getSubscription(), null));
    }

    private Mono<Void> applySubscriptionUpdated(com.stripe.model.Subscription stripeSubscription) {
        SubscriptionStatus status = "active".equals(stripeSubscription.getStatus())
                ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PAST_DUE;
        return subscriptionRepository.findByStripeCustomerId(stripeSubscription.getCustomer())
                .flatMap(existing -> applySubscription(existing.getOrgId(), existing.getPlanId(), status,
                        stripeSubscription.getCustomer(), stripeSubscription.getId(), null));
    }

    private Mono<Void> applySubscriptionCanceled(com.stripe.model.Subscription stripeSubscription) {
        return subscriptionRepository.findByStripeCustomerId(stripeSubscription.getCustomer())
                .flatMap(existing -> applySubscription(existing.getOrgId(), existing.getPlanId(), SubscriptionStatus.CANCELED,
                        stripeSubscription.getCustomer(), stripeSubscription.getId(), null));
    }

    Mono<Void> applySubscription(Long orgId, Long planId, SubscriptionStatus status, String stripeCustomerId,
                                  String stripeSubscriptionId, Instant currentPeriodEnd) {
        return subscriptionRepository.updateForOrg(orgId, planId, status, stripeCustomerId, stripeSubscriptionId,
                currentPeriodEnd, Instant.now()).then();
    }
}
