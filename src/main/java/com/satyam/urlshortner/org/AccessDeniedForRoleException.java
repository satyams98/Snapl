package com.satyam.urlshortner.org;

public class AccessDeniedForRoleException extends RuntimeException {
    public AccessDeniedForRoleException(Role required) {
        super("This action requires the " + required.name() + " role or higher");
    }
}
