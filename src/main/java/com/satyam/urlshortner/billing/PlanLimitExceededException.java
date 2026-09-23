package com.satyam.urlshortner.billing;

public class PlanLimitExceededException extends RuntimeException {
    public PlanLimitExceededException(String resource, int limit) {
        super("Your plan allows up to " + limit + " " + resource + "(s). Upgrade your plan to add more.");
    }
}
