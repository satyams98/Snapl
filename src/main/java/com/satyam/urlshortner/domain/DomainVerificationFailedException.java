package com.satyam.urlshortner.domain;

public class DomainVerificationFailedException extends RuntimeException {
    public DomainVerificationFailedException(String domain) {
        super("No matching TXT record was found for '" + domain + "' yet. DNS changes can take time to propagate.");
    }
}
