package com.satyam.urlshortner.domain;

public class DomainAlreadyExistsException extends RuntimeException {
    public DomainAlreadyExistsException(String domain) {
        super("Domain '" + domain + "' is already registered");
    }
}
