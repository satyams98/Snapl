package com.satyam.urlshortner.url;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkActionRequest(
        @NotEmpty List<String> shortCodes,
        @NotNull BulkAction action,
        Long folderId,
        List<String> tags
) {}
