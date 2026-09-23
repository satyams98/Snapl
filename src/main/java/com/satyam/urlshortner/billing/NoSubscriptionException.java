package com.satyam.urlshortner.billing;

public class NoSubscriptionException extends RuntimeException {
    public NoSubscriptionException(Long orgId) {
        super("No subscription found for organization " + orgId);
    }
}
