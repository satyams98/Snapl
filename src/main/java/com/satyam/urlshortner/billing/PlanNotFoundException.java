package com.satyam.urlshortner.billing;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(String code) {
        super("No plan found with code '" + code + "'");
    }
}
