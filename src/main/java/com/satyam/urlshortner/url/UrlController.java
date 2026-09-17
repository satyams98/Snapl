package com.satyam.urlshortner.url;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    public Mono<ResponseEntity<ShortenResponse>> shorten(@Valid @RequestBody ShortenRequest request) {
        return urlService.shorten(request.longUrl())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{code}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String code) {
        return urlService.resolve(code)
                .map(longUrl -> ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, longUrl)
                        .header(HttpHeaders.CACHE_CONTROL, "no-store")
                        .<Void>build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}