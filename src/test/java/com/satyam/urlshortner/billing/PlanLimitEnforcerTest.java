package com.satyam.urlshortner.billing;

import com.satyam.urlshortner.domain.CustomDomainRepository;
import com.satyam.urlshortner.url.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanLimitEnforcerTest {

    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private UrlRepository urlRepository;
    @Mock
    private CustomDomainRepository customDomainRepository;

    private PlanLimitEnforcer enforcer;

    private static final Long ORG_ID = 1L;

    @BeforeEach
    void setUp() {
        enforcer = new PlanLimitEnforcer(subscriptionService, urlRepository, customDomainRepository);
    }

    @Test
    void checkCanCreateLinkPassesWhenUnderLimit() {
        Plan plan = new Plan(1L, "FREE", "Free", 0, 25, 0, 1000, false, null);
        when(subscriptionService.getCurrentPlan(ORG_ID)).thenReturn(Mono.just(plan));
        when(urlRepository.countByOrgIdAndDisabledAtIsNull(ORG_ID)).thenReturn(Mono.just(10L));

        StepVerifier.create(enforcer.checkCanCreateLink(ORG_ID)).verifyComplete();
    }

    @Test
    void checkCanCreateLinkFailsWhenAtLimit() {
        Plan plan = new Plan(1L, "FREE", "Free", 0, 25, 0, 1000, false, null);
        when(subscriptionService.getCurrentPlan(ORG_ID)).thenReturn(Mono.just(plan));
        when(urlRepository.countByOrgIdAndDisabledAtIsNull(ORG_ID)).thenReturn(Mono.just(25L));

        StepVerifier.create(enforcer.checkCanCreateLink(ORG_ID))
                .expectError(PlanLimitExceededException.class)
                .verify();
    }

    @Test
    void checkCanCreateLinkPassesWhenPlanIsUnlimited() {
        Plan plan = new Plan(3L, "BUSINESS", "Business", 9900, -1, 5, -1, true, null);
        when(subscriptionService.getCurrentPlan(ORG_ID)).thenReturn(Mono.just(plan));

        StepVerifier.create(enforcer.checkCanCreateLink(ORG_ID)).verifyComplete();
    }

    @Test
    void checkCanAddCustomDomainFailsWhenAtLimit() {
        Plan plan = new Plan(1L, "FREE", "Free", 0, 25, 0, 1000, false, null);
        when(subscriptionService.getCurrentPlan(ORG_ID)).thenReturn(Mono.just(plan));
        when(customDomainRepository.countByOrgId(ORG_ID)).thenReturn(Mono.just(0L));

        StepVerifier.create(enforcer.checkCanAddCustomDomain(ORG_ID))
                .expectError(PlanLimitExceededException.class)
                .verify();
    }

    @Test
    void checkApiAccessAllowedFailsForFreePlan() {
        Plan plan = new Plan(1L, "FREE", "Free", 0, 25, 0, 1000, false, null);
        when(subscriptionService.getCurrentPlan(ORG_ID)).thenReturn(Mono.just(plan));

        StepVerifier.create(enforcer.checkApiAccessAllowed(ORG_ID))
                .expectError(PlanFeatureNotAvailableException.class)
                .verify();
    }

    @Test
    void checkApiAccessAllowedPassesForProPlan() {
        Plan plan = new Plan(2L, "PRO", "Pro", 2900, 1000, 1, 50000, true, null);
        when(subscriptionService.getCurrentPlan(ORG_ID)).thenReturn(Mono.just(plan));

        StepVerifier.create(enforcer.checkApiAccessAllowed(ORG_ID)).verifyComplete();
    }
}
