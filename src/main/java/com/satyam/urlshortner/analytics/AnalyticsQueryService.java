package com.satyam.urlshortner.analytics;

import com.satyam.urlshortner.url.ShortUrlNotFoundException;
import com.satyam.urlshortner.url.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private final DatabaseClient databaseClient;
    private final UrlRepository urlRepository;

    public Mono<LinkAnalyticsResponse> getLinkAnalytics(Long orgId, String shortCode, int days) {
        return urlRepository.findByShortCodeAndOrgId(shortCode, orgId)
                .switchIfEmpty(Mono.error(new ShortUrlNotFoundException(shortCode)))
                .flatMap(url -> {
                    LocalDate since = LocalDate.now().minusDays(Math.max(days, 1) - 1L);
                    return Mono.zip(
                            dailySeriesForCode(shortCode, since),
                            totalClicksForCode(shortCode),
                            breakdownForCode(shortCode, "referrer"),
                            breakdownForCode(shortCode, "device_type"),
                            breakdownForCode(shortCode, "browser"),
                            breakdownForCode(shortCode, "os")
                    ).map(t -> new LinkAnalyticsResponse(shortCode, t.getT2(), t.getT1(), t.getT3(), t.getT4(), t.getT5(), t.getT6()));
                });
    }

    public Mono<AnalyticsSummaryResponse> getOrgSummary(Long orgId, int days) {
        LocalDate since = LocalDate.now().minusDays(Math.max(days, 1) - 1L);
        return Mono.zip(
                dailySeriesForOrg(orgId, since),
                totalClicksForOrg(orgId),
                totalLinksForOrg(orgId),
                topLinksForOrg(orgId, since)
        ).map(t -> new AnalyticsSummaryResponse(t.getT2(), t.getT3(), t.getT1(), t.getT4()));
    }

    private Mono<List<DailyClickPoint>> dailySeriesForCode(String shortCode, LocalDate since) {
        return databaseClient.sql("""
                        SELECT day, click_count FROM click_daily_stats
                        WHERE short_code = :shortCode AND day >= :since
                        ORDER BY day
                        """)
                .bind("shortCode", shortCode)
                .bind("since", since)
                .map(row -> new DailyClickPoint(row.get("day", LocalDate.class), row.get("click_count", Long.class)))
                .all()
                .collectList();
    }

    private Mono<Long> totalClicksForCode(String shortCode) {
        return databaseClient.sql("SELECT COUNT(*) AS total FROM click_events WHERE short_code = :shortCode")
                .bind("shortCode", shortCode)
                .map(row -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    // column is always one of a small set of hardcoded literals below, never user input.
    private Mono<List<BreakdownItem>> breakdownForCode(String shortCode, String column) {
        String sql = "SELECT COALESCE(" + column + ", 'Unknown') AS label, COUNT(*) AS total FROM click_events "
                + "WHERE short_code = :shortCode GROUP BY label ORDER BY total DESC LIMIT 10";
        return databaseClient.sql(sql)
                .bind("shortCode", shortCode)
                .map(row -> new BreakdownItem(row.get("label", String.class), row.get("total", Long.class)))
                .all()
                .collectList();
    }

    private Mono<List<DailyClickPoint>> dailySeriesForOrg(Long orgId, LocalDate since) {
        return databaseClient.sql("""
                        SELECT s.day AS day, SUM(s.click_count) AS total FROM click_daily_stats s
                        JOIN urls u ON u.short_code = s.short_code
                        WHERE u.org_id = :orgId AND s.day >= :since
                        GROUP BY s.day
                        ORDER BY s.day
                        """)
                .bind("orgId", orgId)
                .bind("since", since)
                .map(row -> new DailyClickPoint(row.get("day", LocalDate.class), row.get("total", Long.class)))
                .all()
                .collectList();
    }

    private Mono<Long> totalClicksForOrg(Long orgId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) AS total FROM click_events c
                        JOIN urls u ON u.short_code = c.short_code
                        WHERE u.org_id = :orgId
                        """)
                .bind("orgId", orgId)
                .map(row -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private Mono<Long> totalLinksForOrg(Long orgId) {
        return databaseClient.sql("SELECT COUNT(*) AS total FROM urls WHERE org_id = :orgId AND disabled_at IS NULL")
                .bind("orgId", orgId)
                .map(row -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private Mono<List<TopLinkItem>> topLinksForOrg(Long orgId, LocalDate since) {
        return databaseClient.sql("""
                        SELECT u.short_code AS short_code, u.long_url AS long_url, COALESCE(SUM(s.click_count), 0) AS total
                        FROM urls u
                        LEFT JOIN click_daily_stats s ON s.short_code = u.short_code AND s.day >= :since
                        WHERE u.org_id = :orgId
                        GROUP BY u.short_code, u.long_url
                        ORDER BY total DESC
                        LIMIT 5
                        """)
                .bind("orgId", orgId)
                .bind("since", since)
                .map(row -> new TopLinkItem(row.get("short_code", String.class), row.get("long_url", String.class), row.get("total", Long.class)))
                .all()
                .collectList();
    }
}
