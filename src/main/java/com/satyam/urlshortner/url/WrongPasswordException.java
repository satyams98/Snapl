package com.satyam.urlshortner.url;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException(String shortCode) {
        super("Incorrect password for link '" + shortCode + "'");
    }
}
