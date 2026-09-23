package com.satyam.urlshortner.billing;

public class InvalidStripeWebhookSignatureException extends RuntimeException {
    public InvalidStripeWebhookSignatureException() {
        super("Stripe webhook signature verification failed");
    }
}
