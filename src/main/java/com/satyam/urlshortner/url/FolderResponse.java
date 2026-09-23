package com.satyam.urlshortner.url;

public record FolderResponse(Long id, String name) {
    static FolderResponse from(Folder folder) {
        return new FolderResponse(folder.getId(), folder.getName());
    }
}
