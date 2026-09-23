package com.satyam.urlshortner.auth;

import com.satyam.urlshortner.apikey.ApiKeyScope;
import com.satyam.urlshortner.org.Role;

import java.util.Set;

// Security principal resolved from either a validated JWT (dashboard session) or an API key.
// apiKeyScopes is null for JWT sessions (unrestricted, governed by role instead) and non-null
// for API-key sessions, where it restricts which write endpoints the key may call.
public record AuthPrincipal(Long userId, String email, Long orgId, Role role, Set<ApiKeyScope> apiKeyScopes) {

    public static AuthPrincipal forUser(Long userId, String email, Long orgId, Role role) {
        return new AuthPrincipal(userId, email, orgId, role, null);
    }

    public static AuthPrincipal forApiKey(Long orgId, Set<ApiKeyScope> scopes) {
        return new AuthPrincipal(null, null, orgId, Role.MEMBER, scopes);
    }

    public boolean hasWriteAccess() {
        return apiKeyScopes == null || apiKeyScopes.contains(ApiKeyScope.WRITE);
    }
}
