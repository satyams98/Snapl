package com.satyam.urlshortner.apikey;

import java.time.Instant;
import java.util.List;

public record ApiKeyResponse(
        Long id,
        String name,
        String keyPrefix,
        List<String> scopes,
        boolean revoked,
        Instant createdAt,
        Instant lastUsedAt
) {
    static ApiKeyResponse from(ApiKey key) {
        return new ApiKeyResponse(key.getId(), key.getName(), key.getKeyPrefix(),
                List.of(key.getScopes().split(",")), key.getRevokedAt() != null, key.getCreatedAt(), key.getLastUsedAt());
    }
}
