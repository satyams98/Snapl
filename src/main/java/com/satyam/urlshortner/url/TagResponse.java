package com.satyam.urlshortner.url;

public record TagResponse(Long id, String name) {
    static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
