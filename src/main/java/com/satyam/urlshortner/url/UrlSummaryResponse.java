package com.satyam.urlshortner.url;

import java.time.Instant;
import java.util.List;

public record UrlSummaryResponse(
        String shortCode,
        String shortUrl,
        String longUrl,
        Instant createdAt,
        Instant expiresAt,
        boolean disabled,
        String folderName,
        List<String> tags
) {}
