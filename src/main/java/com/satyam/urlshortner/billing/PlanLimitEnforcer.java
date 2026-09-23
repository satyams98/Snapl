package com.satyam.urlshortner.billing;

import com.satyam.urlshortner.domain.CustomDomainRepository;
import com.satyam.urlshortner.url.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PlanLimitEnforcer {

    private final SubscriptionService subscriptionService;
    private final UrlRepository urlRepository;
    private final CustomDomainRepository customDomainRepository;

    public Mono<Void> checkCanCreateLink(Long orgId) {
        return subscriptionService.getCurrentPlan(orgId)
                .flatMap(plan -> plan.isUnlimitedLinks()
                        ? Mono.<Void>empty()
                        : urlRepository.countByOrgIdAndDisabledAtIsNull(orgId)
                                .flatMap(count -> count >= plan.getMaxLinks()
                                        ? Mono.error(new PlanLimitExceededException("active link", plan.getMaxLinks()))
                                        : Mono.empty()));
    }

    public Mono<Void> checkCanAddCustomDomain(Long orgId) {
        return subscriptionService.getCurrentPlan(orgId)
                .flatMap(plan -> customDomainRepository.countByOrgId(orgId)
                        .flatMap(count -> count >= plan.getMaxCustomDomains()
                                ? Mono.error(new PlanLimitExceededException("custom domain", plan.getMaxCustomDomains()))
                                : Mono.empty()));
    }

    public Mono<Void> checkApiAccessAllowed(Long orgId) {
        return subscriptionService.getCurrentPlan(orgId)
                .flatMap(plan -> plan.isApiAccessAllowed()
                        ? Mono.empty()
                        : Mono.error(new PlanFeatureNotAvailableException("API access")));
    }
}
