package com.satyam.urlshortner.apikey;

import java.time.Instant;
import java.util.List;

// The plaintext key is only ever present in this create response; it cannot be retrieved again afterward.
public record ApiKeyCreatedResponse(Long id, String name, String apiKey, List<String> scopes, Instant createdAt) {}
