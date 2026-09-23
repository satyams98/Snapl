package com.satyam.urlshortner.org;

public record MemberResponse(Long userId, String email, String name, Role role) {
}
