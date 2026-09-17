package com.satyam.urlshortner.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimitConfig {
}
