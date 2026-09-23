package com.satyam.urlshortner.apikey;

public class ApiKeyNotFoundException extends RuntimeException {
    public ApiKeyNotFoundException(Long id) {
        super("No API key found with id " + id);
    }
}
