package com.satyam.urlshortner.url;

import com.satyam.urlshortner.analytics.ClickEvent;
import com.satyam.urlshortner.analytics.ClickEventPublisher;
import com.satyam.urlshortner.auth.CurrentUser;
import com.satyam.urlshortner.auth.InsufficientScopeException;
import com.satyam.urlshortner.domain.DomainResolutionFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final ClickEventPublisher clickEventPublisher;

    @Value("${app.frontend.origin}")
    private String frontendOrigin;

    @PostMapping("/shorten")
    public Mono<ResponseEntity<ShortenResponse>> shorten(@Valid @RequestBody ShortenRequest request) {
        return CurrentUser.get()
                .flatMap(principal -> principal.hasWriteAccess()
                        ? urlService.shorten(request.longUrl(), request.customAlias(), principal.orgId(), request.startsAt(), request.password())
                        : Mono.error(new InsufficientScopeException()))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{code}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String code, ServerHttpRequest request, ServerWebExchange exchange) {
        Long restrictToOrgId = exchange.getAttribute(DomainResolutionFilter.RESOLVED_ORG_ID_ATTRIBUTE);
        return urlService.resolve(code, restrictToOrgId)
                .map(resolved -> {
                    if (resolved.passwordProtected()) {
                        // The actual click is counted once the visitor successfully unlocks the link, not here.
                        return ResponseEntity.status(HttpStatus.FOUND)
                                .header(HttpHeaders.LOCATION, frontendOrigin + "/unlock/" + code)
                                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                                .<Void>build();
                    }
                    clickEventPublisher.publish(buildClickEvent(code, request));
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .header(HttpHeaders.LOCATION, resolved.longUrl())
                            .header(HttpHeaders.CACHE_CONTROL, "no-store")
                            .<Void>build();
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/{code}/unlock")
    public Mono<ResponseEntity<UnlockResponse>> unlock(@PathVariable String code, @Valid @RequestBody UnlockRequest request,
                                                        ServerHttpRequest httpRequest) {
        return urlService.unlock(code, request.password())
                .map(longUrl -> {
                    clickEventPublisher.publish(buildClickEvent(code, httpRequest));
                    return ResponseEntity.ok(new UnlockResponse(longUrl));
                });
    }

    @DeleteMapping("/{code}")
    public Mono<ResponseEntity<Void>> disable(@PathVariable String code) {
        return CurrentUser.get()
                .flatMap(principal -> principal.hasWriteAccess()
                        ? urlService.disable(code, principal.orgId())
                        : Mono.error(new InsufficientScopeException()))
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    private ClickEvent buildClickEvent(String code, ServerHttpRequest request) {
        String ip = request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : null;
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        String referrer = request.getHeaders().getFirst(HttpHeaders.REFERER);
        return new ClickEvent(code, Instant.now(), ip, userAgent, referrer);
    }
}