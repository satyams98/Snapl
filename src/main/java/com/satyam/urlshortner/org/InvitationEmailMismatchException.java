package com.satyam.urlshortner.org;

public class InvitationEmailMismatchException extends RuntimeException {
    public InvitationEmailMismatchException() {
        super("This invitation was issued to a different email address");
    }
}
