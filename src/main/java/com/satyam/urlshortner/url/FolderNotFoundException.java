package com.satyam.urlshortner.url;

public class FolderNotFoundException extends RuntimeException {
    public FolderNotFoundException(Long folderId) {
        super("No folder found with id " + folderId);
    }
}
