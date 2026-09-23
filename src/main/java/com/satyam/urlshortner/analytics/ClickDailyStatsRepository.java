package com.satyam.urlshortner.analytics;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface ClickDailyStatsRepository extends ReactiveCrudRepository<ClickDailyStatsEntity, Long> {

    @Modifying
    @Query("""
            INSERT INTO click_daily_stats (short_code, day, click_count) VALUES (:shortCode, :day, 1)
            ON CONFLICT (short_code, day) DO UPDATE SET click_count = click_daily_stats.click_count + 1
            """)
    Mono<Integer> increment(String shortCode, LocalDate day);
}
