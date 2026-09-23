package com.satyam.urlshortner.domain;

public class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(Long id) {
        super("No domain found with id " + id);
    }
}
