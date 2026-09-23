package com.satyam.urlshortner.org;

public class InvitationExpiredException extends RuntimeException {
    public InvitationExpiredException(String token) {
        super("Invitation '" + token + "' has expired");
    }
}
