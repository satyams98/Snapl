package com.satyam.urlshortner.billing;

public record PlanResponse(
        String code,
        String name,
        int priceCents,
        int maxLinks,
        int maxCustomDomains,
        int maxClicksPerMonth,
        boolean apiAccessAllowed
) {
    static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getCode(), plan.getName(), plan.getPriceCents(), plan.getMaxLinks(),
                plan.getMaxCustomDomains(), plan.getMaxClicksPerMonth(), plan.isApiAccessAllowed());
    }
}
