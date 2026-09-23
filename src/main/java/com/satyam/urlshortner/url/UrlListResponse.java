package com.satyam.urlshortner.url;

import java.util.List;

public record UrlListResponse(List<UrlSummaryResponse> items, long totalCount, int page, int pageSize) {}
