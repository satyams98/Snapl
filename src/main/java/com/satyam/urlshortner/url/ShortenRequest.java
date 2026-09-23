package com.satyam.urlshortner.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ShortenRequest(
        @NotBlank(message = "longUrl must not be blank")
        @Pattern(regexp = "^https?://.+", message = "longUrl must start with http:// or https://")
        String longUrl,

        @Pattern(regexp = "^[A-Za-z0-9_-]{3,20}$", message = "customAlias must be 3-20 characters: letters, digits, hyphens, or underscores")
        String customAlias,

        Instant startsAt,

        @Size(min = 4, max = 72, message = "password must be 4-72 characters")
        String password
) {}

