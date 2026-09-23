package com.satyam.urlshortner.org;

public class InvitationNotFoundException extends RuntimeException {
    public InvitationNotFoundException(String token) {
        super("No pending invitation found for token '" + token + "'");
    }
}
