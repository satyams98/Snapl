package com.satyam.urlshortner.billing;

public record UsageSummaryResponse(
        int activeLinks,
        int maxLinks,
        int customDomains,
        int maxCustomDomains,
        boolean apiAccessAllowed,
        String planCode,
        String planName,
        String subscriptionStatus
) {}
