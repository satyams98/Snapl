package com.satyam.urlshortner.billing;

// Thrown whenever a Stripe-backed action is attempted without app.stripe.secret-key configured
// (e.g. local/dev environments) — kept distinct from real Stripe API failures.
public class StripeNotConfiguredException extends RuntimeException {
    public StripeNotConfiguredException() {
        super("Billing is not configured on this deployment yet. Set app.stripe.secret-key to enable checkout.");
    }
}
