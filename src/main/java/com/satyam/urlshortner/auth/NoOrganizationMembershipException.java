package com.satyam.urlshortner.auth;

public class NoOrganizationMembershipException extends RuntimeException {
    public NoOrganizationMembershipException(Long userId) {
        super("User " + userId + " does not belong to any organization");
    }
}
