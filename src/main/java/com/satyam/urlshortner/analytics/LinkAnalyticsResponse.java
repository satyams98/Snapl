package com.satyam.urlshortner.analytics;

import java.util.List;

public record LinkAnalyticsResponse(
        String shortCode,
        long totalClicks,
        List<DailyClickPoint> series,
        List<BreakdownItem> referrers,
        List<BreakdownItem> devices,
        List<BreakdownItem> browsers,
        List<BreakdownItem> operatingSystems
) {}
