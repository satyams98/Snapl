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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

    // Prevents custom aliases from shadowing real application routes.
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "shorten", "actuator", "favicon.ico", "robots.txt", "health");

    public Mono<ShortenResponse> shorten(String longUrl, String customAlias, Long orgId) {
        if (customAlias != null) {
            return createCustomAlias(longUrl, customAlias, orgId);
        }

        String longUrlHash = UrlHasher.sha256Hex(longUrl);

        return repository.findFirstByLongUrlHashAndOrgId(longUrlHash, orgId)
                .flatMap(existing -> cache(existing).then(Mono.just(toResponse(existing))))
                .switchIfEmpty(Mono.defer(() -> createShortUrl(longUrl, longUrlHash, orgId)));
    }

    private Mono<ShortenResponse> createCustomAlias(String longUrl, String customAlias, Long orgId) {
        if (RESERVED_ALIASES.contains(customAlias.toLowerCase())) {
            return Mono.error(new ReservedAliasException(customAlias));
        }

        return repository.findByShortCode(customAlias)
                .<ShortenResponse>flatMap(existing -> Mono.error(new AliasAlreadyExistsException(customAlias)))
                .switchIfEmpty(Mono.defer(() -> {
                    long id = idGenerator.nextId();
                    String longUrlHash = UrlHasher.sha256Hex(longUrl);
                    UrlEntity entity = new UrlEntity(id, customAlias, longUrl, longUrlHash, Instant.now(), null, true, null, orgId, null);
                    return repository.save(entity).flatMap(saved -> cache(saved).then(Mono.just(toResponse(saved))));
                }));
    }

    private Mono<ShortenResponse> createShortUrl(String longUrl, String longUrlHash, Long orgId) {
        long id = idGenerator.nextId();
        String shortCode = Base62Encoder.encode(id);
        UrlEntity entity = new UrlEntity(id, shortCode, longUrl, longUrlHash, Instant.now(), null, false, null, orgId, null);

        return repository.save(entity)
                .flatMap(saved -> cache(saved).then(Mono.just(toResponse(saved))));
    }

    public Mono<String> resolve(String shortCode, Long restrictToOrgId) {
        if (restrictToOrgId != null) {
            // Custom-domain traffic bypasses the cache so branded domains can't serve another org's link.
            return repository.findByShortCode(shortCode)
                    .filter(entity -> !isBlocked(entity) && restrictToOrgId.equals(entity.getOrgId()))
                    .map(UrlEntity::getLongUrl);
        }
        return redisTemplate.opsForValue().get(shortCode)
                .switchIfEmpty(Mono.defer(() ->
                        repository.findByShortCode(shortCode)
                                .filter(entity -> !isBlocked(entity))
                                .flatMap(entity -> cache(entity).then(Mono.just(entity.getLongUrl())))
                ));
    }

    public Mono<Void> disable(String shortCode, Long orgId) {
        return repository.disableByShortCodeAndOrgId(shortCode, orgId, Instant.now())
                .flatMap(rowsUpdated -> rowsUpdated > 0
                        ? redisTemplate.delete(shortCode).then()
                        : Mono.error(new ShortUrlNotFoundException(shortCode)));
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

    private boolean isBlocked(UrlEntity entity) {
        return isExpired(entity) || entity.getDisabledAt() != null;
    }

    private ShortenResponse toResponse(UrlEntity entity) {
        return new ShortenResponse(entity.getShortCode(), baseUrl + "/" + entity.getShortCode(), entity.getLongUrl());
    }
}