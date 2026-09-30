package com.satyam.urlshortner.org;

import com.satyam.urlshortner.auth.User;

public record AcceptedInvitation(User user, Membership membership, Organization organization) {}
