package com.satyam.urlshortner.analytics;

import com.satyam.urlshortner.auth.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    @GetMapping("/summary")
    public Mono<AnalyticsSummaryResponse> summary(@RequestParam(defaultValue = "30") int days) {
        return CurrentUser.get().flatMap(principal -> analyticsQueryService.getOrgSummary(principal.orgId(), days));
    }
}
