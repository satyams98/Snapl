package com.satyam.urlshortner.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;

// Null fields mean "leave unchanged"; an empty tags list clears all tags; an empty password string clears password protection.
public record UpdateUrlRequest(
        @Pattern(regexp = "^https?://.+", message = "longUrl must start with http:// or https://") String longUrl,
        Instant expiresAt,
        Long folderId,
        List<@NotBlank String> tags,
        Instant startsAt,
        String password
) {}
