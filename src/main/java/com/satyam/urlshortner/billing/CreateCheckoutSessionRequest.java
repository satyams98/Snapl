package com.satyam.urlshortner.billing;

import jakarta.validation.constraints.NotBlank;

public record CreateCheckoutSessionRequest(@NotBlank String planCode) {}
