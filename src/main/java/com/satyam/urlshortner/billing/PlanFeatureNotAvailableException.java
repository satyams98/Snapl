package com.satyam.urlshortner.billing;

public class PlanFeatureNotAvailableException extends RuntimeException {
    public PlanFeatureNotAvailableException(String feature) {
        super(feature + " is not available on your current plan. Upgrade your plan to unlock it.");
    }
}
