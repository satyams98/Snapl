package com.satyam.urlshortner.url;

import com.satyam.urlshortner.analytics.ClickEvent;
import com.satyam.urlshortner.analytics.ClickEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final ClickEventPublisher clickEventPublisher;

    @PostMapping("/shorten")
    public Mono<ResponseEntity<ShortenResponse>> shorten(@Valid @RequestBody ShortenRequest request) {
        return urlService.shorten(request.longUrl(), request.customAlias())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{code}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String code, ServerHttpRequest request) {
        return urlService.resolve(code)
                .map(longUrl -> {
                    clickEventPublisher.publish(buildClickEvent(code, request));
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .header(HttpHeaders.LOCATION, longUrl)
                            .header(HttpHeaders.CACHE_CONTROL, "no-store")
                            .<Void>build();
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{code}")
    public Mono<ResponseEntity<Void>> disable(@PathVariable String code) {
        return urlService.disable(code)
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