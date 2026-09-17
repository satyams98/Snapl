package com.satyam.urlshortner.url;

public class ShortUrlNotFoundException extends RuntimeException {
    public ShortUrlNotFoundException(String shortCode) {
        super("No active short URL found for code: " + shortCode);
    }
}
