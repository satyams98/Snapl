package com.satyam.urlshortner.auth;

import com.satyam.urlshortner.org.Role;

// Security principal resolved from a validated JWT access token; org/role reflect the org active at issuance.
public record AuthPrincipal(Long userId, String email, Long orgId, Role role) {
}
