package com.satyam.urlshortner.analytics;

import java.time.Instant;

public record ClickEvent(String shortCode, Instant clickedAt, String ipAddress, String userAgent, String referrer) {}
