package com.satyam.urlshortner.url;

import jakarta.validation.constraints.NotBlank;

public record UnlockRequest(@NotBlank String password) {}
