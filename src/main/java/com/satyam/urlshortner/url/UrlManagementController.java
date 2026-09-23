package com.satyam.urlshortner.url;

import com.satyam.urlshortner.analytics.AnalyticsQueryService;
import com.satyam.urlshortner.analytics.LinkAnalyticsResponse;
import com.satyam.urlshortner.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlManagementController {

    private final UrlQueryService urlQueryService;
    private final UrlBulkActionService urlBulkActionService;
    private final AnalyticsQueryService analyticsQueryService;

    @GetMapping
    public Mono<UrlListResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        return CurrentUser.get()
                .flatMap(principal -> urlQueryService.list(principal.orgId(), search, folderId, status, tag, Math.max(page, 0), boundedSize));
    }

    @GetMapping("/{code}")
    public Mono<UrlSummaryResponse> detail(@PathVariable String code) {
        return CurrentUser.get().flatMap(principal -> urlQueryService.getDetail(principal.orgId(), code));
    }

    @PatchMapping("/{code}")
    public Mono<UrlSummaryResponse> update(@PathVariable String code, @Valid @RequestBody UpdateUrlRequest request) {
        return CurrentUser.get().flatMap(principal -> urlQueryService.update(principal.orgId(), code, request));
    }

    @GetMapping("/{code}/analytics")
    public Mono<LinkAnalyticsResponse> analytics(@PathVariable String code, @RequestParam(defaultValue = "30") int days) {
        return CurrentUser.get().flatMap(principal -> analyticsQueryService.getLinkAnalytics(principal.orgId(), code, days));
    }

    @PostMapping("/bulk")
    public Mono<BulkActionResult> bulk(@Valid @RequestBody BulkActionRequest request) {
        return CurrentUser.get().flatMap(principal -> urlBulkActionService.execute(principal.orgId(), request));
    }
}
