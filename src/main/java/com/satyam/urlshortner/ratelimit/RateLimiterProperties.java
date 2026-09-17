package com.satyam.urlshortner.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimiterProperties(long capacity, double refillTokensPerSecond) {

    public RateLimiterProperties {
        if (capacity <= 0) {
            throw new IllegalArgumentException("app.rate-limit.capacity must be positive");
        }
        if (refillTokensPerSecond <= 0) {
            throw new IllegalArgumentException("app.rate-limit.refill-tokens-per-second must be positive");
        }
    }
}
