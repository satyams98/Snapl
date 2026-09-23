package com.satyam.urlshortner.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateDomainRequest(
        @NotBlank
        @Pattern(
                regexp = "^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))+$",
                message = "domain must be a valid hostname, e.g. links.example.com")
        String domain
) {}
