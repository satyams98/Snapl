package com.satyam.urlshortner.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateApiKeyRequest(
        @NotBlank @Size(max = 120) String name,
        @NotEmpty List<ApiKeyScope> scopes
) {}
