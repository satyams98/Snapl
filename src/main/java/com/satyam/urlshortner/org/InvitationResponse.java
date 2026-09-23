package com.satyam.urlshortner.org;

import java.time.Instant;

// The token is only ever returned to an authenticated Owner/Admin caller; delivering it to the
// invitee (e.g. via email) is out of scope for this phase.
public record InvitationResponse(Long id, String email, Role role, String token, Instant expiresAt) {
}
