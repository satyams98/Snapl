package com.satyam.urlshortner.billing;

public class StripePriceNotConfiguredException extends RuntimeException {
    public StripePriceNotConfiguredException(String planCode) {
        super("Plan '" + planCode + "' has no Stripe price configured yet");
    }
}
