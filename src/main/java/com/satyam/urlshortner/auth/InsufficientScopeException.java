package com.satyam.urlshortner.auth;

public class InsufficientScopeException extends RuntimeException {
    public InsufficientScopeException() {
        super("This API key does not have the WRITE scope required for this action");
    }
}
