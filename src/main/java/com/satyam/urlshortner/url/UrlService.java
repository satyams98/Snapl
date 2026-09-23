package com.satyam.urlshortner.url;

import com.satyam.urlshortner.idgen.Base62Encoder;
import com.satyam.urlshortner.idgen.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

    // Prevents custom aliases from shadowing real application routes.
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "shorten", "actuator", "favicon.ico", "robots.txt", "health");

    public Mono<ShortenResponse> shorten(String longUrl, String customAlias, Long orgId) {
        return shorten(longUrl, customAlias, orgId, null, null);
    }

    public Mono<ShortenResponse> shorten(String longUrl, String customAlias, Long orgId, Instant startsAt, String rawPassword) {
        String passwordHash = rawPassword != null && !rawPassword.isBlank() ? passwordEncoder.encode(rawPassword) : null;
        if (customAlias != null) {
            return createCustomAlias(longUrl, customAlias, orgId, startsAt, passwordHash);
        }

        String longUrlHash = UrlHasher.sha256Hex(longUrl);

        return repository.findFirstByLongUrlHashAndOrgId(longUrlHash, orgId)
                .flatMap(existing -> cache(existing).then(Mono.just(toResponse(existing))))
                .switchIfEmpty(Mono.defer(() -> createShortUrl(longUrl, longUrlHash, orgId, startsAt, passwordHash)));
    }

    private Mono<ShortenResponse> createCustomAlias(String longUrl, String customAlias, Long orgId, Instant startsAt, String passwordHash) {
        if (RESERVED_ALIASES.contains(customAlias.toLowerCase())) {
            return Mono.error(new ReservedAliasException(customAlias));
        }

        return repository.findByShortCode(customAlias)
                .<ShortenResponse>flatMap(existing -> Mono.error(new AliasAlreadyExistsException(customAlias)))
                .switchIfEmpty(Mono.defer(() -> {
                    long id = idGenerator.nextId();
                    String longUrlHash = UrlHasher.sha256Hex(longUrl);
                    UrlEntity entity = new UrlEntity(id, customAlias, longUrl, longUrlHash, Instant.now(), null, true, null,
                            orgId, null, startsAt, passwordHash);
                    return repository.save(entity).flatMap(saved -> cache(saved).then(Mono.just(toResponse(saved))));
                }));
    }

    private Mono<ShortenResponse> createShortUrl(String longUrl, String longUrlHash, Long orgId, Instant startsAt, String passwordHash) {
        long id = idGenerator.nextId();
        String shortCode = Base62Encoder.encode(id);
        UrlEntity entity = new UrlEntity(id, shortCode, longUrl, longUrlHash, Instant.now(), null, false, null,
                orgId, null, startsAt, passwordHash);

        return repository.save(entity)
                .flatMap(saved -> cache(saved).then(Mono.just(toResponse(saved))));
    }

    public Mono<ResolvedLink> resolve(String shortCode, Long restrictToOrgId) {
        if (restrictToOrgId != null) {
            // Custom-domain traffic bypasses the cache so branded domains can't serve another org's link.
            return repository.findByShortCode(shortCode)
                    .filter(entity -> !isBlocked(entity) && restrictToOrgId.equals(entity.getOrgId()))
                    .map(this::toResolvedLink);
        }
        return redisTemplate.opsForValue().get(shortCode)
                .map(cachedLongUrl -> new ResolvedLink(cachedLongUrl, false))
                .switchIfEmpty(Mono.defer(() ->
                        repository.findByShortCode(shortCode)
                                .filter(entity -> !isBlocked(entity))
                                .flatMap(entity -> cache(entity).then(Mono.just(toResolvedLink(entity))))
                ));
    }

    // Used by the public unlock endpoint; verifies the password against the stored hash and returns the destination.
    public Mono<String> unlock(String shortCode, String rawPassword) {
        return repository.findByShortCode(shortCode)
                .filter(entity -> !isBlocked(entity))
                .switchIfEmpty(Mono.error(new ShortUrlNotFoundException(shortCode)))
                .flatMap(entity -> entity.getPasswordHash() == null || passwordEncoder.matches(rawPassword, entity.getPasswordHash())
                        ? Mono.just(entity.getLongUrl())
                        : Mono.error(new WrongPasswordException(shortCode)));
    }

    public Mono<Void> disable(String shortCode, Long orgId) {
        return repository.disableByShortCodeAndOrgId(shortCode, orgId, Instant.now())
                .flatMap(rowsUpdated -> rowsUpdated > 0
                        ? redisTemplate.delete(shortCode).then()
                        : Mono.error(new ShortUrlNotFoundException(shortCode)));
    }

    // Called after edits so a stale cached longUrl (or protection status) is never served.
    public Mono<Void> evictCache(String shortCode) {
        return redisTemplate.delete(shortCode).then();
    }

    private Mono<Void> cache(UrlEntity entity) {
        if (entity.getPasswordHash() != null || isNotYetActive(entity)) {
            return Mono.empty();
        }
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

    private boolean isNotYetActive(UrlEntity entity) {
        return entity.getStartsAt() != null && entity.getStartsAt().isAfter(Instant.now());
    }

    private boolean isBlocked(UrlEntity entity) {
        return isExpired(entity) || entity.getDisabledAt() != null || isNotYetActive(entity);
    }

    private ResolvedLink toResolvedLink(UrlEntity entity) {
        return new ResolvedLink(entity.getLongUrl(), entity.getPasswordHash() != null);
    }

    private ShortenResponse toResponse(UrlEntity entity) {
        return new ShortenResponse(entity.getShortCode(), baseUrl + "/" + entity.getShortCode(), entity.getLongUrl());
    }
}
