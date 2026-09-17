package com.satyam.urlshortner.url;

import com.satyam.urlshortner.idgen.Base62Encoder;
import com.satyam.urlshortner.idgen.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

    public Mono<ShortenResponse> shorten(String longUrl) {
        String longUrlHash = UrlHasher.sha256Hex(longUrl);

        return repository.findFirstByLongUrlHash(longUrlHash)
                .flatMap(existing -> cache(existing).then(Mono.just(toResponse(existing))))
                .switchIfEmpty(Mono.defer(() -> createShortUrl(longUrl, longUrlHash)));
    }

    private Mono<ShortenResponse> createShortUrl(String longUrl, String longUrlHash) {
        long id = idGenerator.nextId();
        String shortCode = Base62Encoder.encode(id);
        UrlEntity entity = new UrlEntity(id, shortCode, longUrl, longUrlHash, Instant.now(), null, false);

        return repository.save(entity)
                .flatMap(saved -> cache(saved).then(Mono.just(toResponse(saved))));
    }

    public Mono<String> resolve(String shortCode) {
        return redisTemplate.opsForValue().get(shortCode)
                .switchIfEmpty(Mono.defer(() ->
                        repository.findByShortCode(shortCode)
                                .filter(entity -> !isExpired(entity))
                                .flatMap(entity -> cache(entity).then(Mono.just(entity.getLongUrl())))
                ));
    }

    private Mono<Void> cache(UrlEntity entity) {
        Duration ttl = ttlFor(entity);
        if (ttl.isZero() || ttl.isNegative()) {
            return Mono.empty();
        }
        return redisTemplate.opsForValue().set(entity.getShortCode(), entity.getLongUrl(), ttl).then();
    }

    private Duration ttlFor(UrlEntity entity) {
        if (entity.getExpiresAt() == null) {
            return DEFAULT_CACHE_TTL;
        }
        Duration remaining = Duration.between(Instant.now(), entity.getExpiresAt());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private boolean isExpired(UrlEntity entity) {
        return entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now());
    }

    private ShortenResponse toResponse(UrlEntity entity) {
        return new ShortenResponse(entity.getShortCode(), baseUrl + "/" + entity.getShortCode(), entity.getLongUrl());
    }
}