package com.satyam.urlshortner.billing;

import com.satyam.urlshortner.auth.CurrentUser;
import com.satyam.urlshortner.domain.CustomDomainRepository;
import com.satyam.urlshortner.url.UrlRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final PlanRepository planRepository;
    private final SubscriptionService subscriptionService;
    private final UrlRepository urlRepository;
    private final CustomDomainRepository customDomainRepository;
    private final StripeCheckoutService stripeCheckoutService;
    private final StripeWebhookService stripeWebhookService;

    @GetMapping("/plans")
    public Flux<PlanResponse> plans() {
        return planRepository.findAllByOrderByPriceCentsAsc().map(PlanResponse::from);
    }

    @GetMapping("/summary")
    public Mono<UsageSummaryResponse> summary() {
        return CurrentUser.get().flatMap(principal -> {
            Long orgId = principal.orgId();
            return Mono.zip(
                    subscriptionService.getSubscription(orgId),
                    subscriptionService.getCurrentPlan(orgId),
                    urlRepository.countByOrgIdAndDisabledAtIsNull(orgId),
                    customDomainRepository.countByOrgId(orgId)
            ).map(t -> new UsageSummaryResponse(
                    t.getT3().intValue(), t.getT2().getMaxLinks(),
                    t.getT4().intValue(), t.getT2().getMaxCustomDomains(),
                    t.getT2().isApiAccessAllowed(), t.getT2().getCode(), t.getT2().getName(),
                    t.getT1().getStatus().name()));
        });
    }

    @PostMapping("/checkout-session")
    public Mono<CheckoutSessionResponse> checkout(@Valid @RequestBody CreateCheckoutSessionRequest request) {
        return CurrentUser.get()
                .flatMap(principal -> stripeCheckoutService.createCheckoutSessionUrl(principal.orgId(), request.planCode()))
                .map(CheckoutSessionResponse::new);
    }

    @PostMapping("/webhook")
    public Mono<ResponseEntity<Void>> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        return stripeWebhookService.handleWebhook(payload, signature)
                .thenReturn(ResponseEntity.ok().<Void>build());
    }
}
