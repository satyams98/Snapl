package com.satyam.urlshortner.analytics;

import java.util.List;

public record AnalyticsSummaryResponse(
        long totalClicks,
        long totalLinks,
        List<DailyClickPoint> series,
        List<TopLinkItem> topLinks
) {}
