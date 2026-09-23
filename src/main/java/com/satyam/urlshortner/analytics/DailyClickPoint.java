package com.satyam.urlshortner.analytics;

import java.time.LocalDate;

public record DailyClickPoint(LocalDate day, long clicks) {}
