package com.satyam.urlshortner.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShortenRequest(
        @NotBlank(message = "longUrl must not be blank")
        @Pattern(regexp = "^https?://.+", message = "longUrl must start with http:// or https://")
        String longUrl,

        @Pattern(regexp = "^[A-Za-z0-9_-]{3,20}$", message = "customAlias must be 3-20 characters: letters, digits, hyphens, or underscores")
        String customAlias
) {}

