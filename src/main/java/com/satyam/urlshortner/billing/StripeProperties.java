package com.satyam.urlshortner.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

// All fields default to blank in application.yaml; Stripe-backed endpoints are disabled until real
// keys/price IDs are configured (see StripeNotConfiguredException), so this ships safely with no credentials.
@ConfigurationProperties(prefix = "app.stripe")
public record StripeProperties(String secretKey, String webhookSecret, String successUrl, String cancelUrl) {
}
