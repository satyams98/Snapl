package com.satyam.urlshortner.url;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.satyam.urlshortner.idgen.SnowflakeIdGenerator;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository repository;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(repository, new SnowflakeIdGenerator(1), redisTemplate);
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void shortenReusesExistingEntryForSameLongUrl() {
        String longUrl = "https://example.com/some/very/long/path";
        String hash = UrlHasher.sha256Hex(longUrl);
        UrlEntity existing = new UrlEntity(1L, "abc123", longUrl, hash, Instant.now(), null, false, null);

        when(repository.findFirstByLongUrlHash(hash)).thenReturn(Mono.just(existing));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(urlService.shorten(longUrl, null))
                .assertNext(response -> {
                    assertEquals("abc123", response.shortCode());
                    assertEquals("http://localhost:8080/abc123", response.shortUrl());
                })
                .verifyComplete();

        verify(repository, never()).save(any());
    }

    @Test
    void shortenCreatesNewEntryWhenNoneExists() {
        String longUrl = "https://example.com/brand-new";

        when(repository.findFirstByLongUrlHash(anyString())).thenReturn(Mono.empty());
        when(repository.save(any(UrlEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(urlService.shorten(longUrl, null))
                .assertNext(response -> assertEquals(longUrl, response.longUrl()))
                .verifyComplete();

        verify(repository).save(any(UrlEntity.class));
    }

    @Test
    void shortenWithCustomAliasCreatesEntryWithGivenCode() {
        String longUrl = "https://example.com/custom";

        when(repository.findByShortCode("my-alias")).thenReturn(Mono.empty());
        when(repository.save(any(UrlEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(urlService.shorten(longUrl, "my-alias"))
                .assertNext(response -> assertEquals("my-alias", response.shortCode()))
                .verifyComplete();
    }

    @Test
    void shortenWithTakenAliasFails() {
        String longUrl = "https://example.com/custom";
        UrlEntity existing = new UrlEntity(1L, "taken", "https://other.example.com", "hash", Instant.now(), null, true, null);

        when(repository.findByShortCode("taken")).thenReturn(Mono.just(existing));

        StepVerifier.create(urlService.shorten(longUrl, "taken"))
                .expectError(AliasAlreadyExistsException.class)
                .verify();
    }

    @Test
    void shortenWithReservedAliasFails() {
        StepVerifier.create(urlService.shorten("https://example.com", "shorten"))
                .expectError(ReservedAliasException.class)
                .verify();
    }

    @Test
    void resolveReturnsEmptyForExpiredLink() {
        String shortCode = "expired1";
        UrlEntity expired = new UrlEntity(1L, shortCode, "https://example.com", "hash",
                Instant.now().minusSeconds(3600), Instant.now().minusSeconds(60), false, null);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(shortCode)).thenReturn(Mono.empty());
        when(repository.findByShortCode(shortCode)).thenReturn(Mono.just(expired));

        StepVerifier.create(urlService.resolve(shortCode))
                .verifyComplete();
    }

    @Test
    void resolveReturnsEmptyForDisabledLink() {
        String shortCode = "disabled1";
        UrlEntity disabled = new UrlEntity(1L, shortCode, "https://example.com", "hash",
                Instant.now().minusSeconds(3600), null, false, Instant.now().minusSeconds(10));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(shortCode)).thenReturn(Mono.empty());
        when(repository.findByShortCode(shortCode)).thenReturn(Mono.just(disabled));

        StepVerifier.create(urlService.resolve(shortCode))
                .verifyComplete();
    }

    @Test
    void resolveReturnsCachedLongUrlWithoutHittingDatabase() {
        String shortCode = "cached1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(shortCode)).thenReturn(Mono.just("https://example.com/cached"));

        StepVerifier.create(urlService.resolve(shortCode))
                .expectNext("https://example.com/cached")
                .verifyComplete();

        verify(repository, never()).findByShortCode(anyString());
    }

    @Test
    void disableEvictsCacheWhenRowUpdated() {
        String shortCode = "abc123";
        when(repository.disableByShortCode(eq(shortCode), any(Instant.class))).thenReturn(Mono.just(1));
        when(redisTemplate.delete(shortCode)).thenReturn(Mono.just(1L));

        StepVerifier.create(urlService.disable(shortCode)).verifyComplete();

        verify(redisTemplate).delete(shortCode);
    }

    @Test
    void disableFailsWhenCodeDoesNotExist() {
        String shortCode = "missing";
        when(repository.disableByShortCode(eq(shortCode), any(Instant.class))).thenReturn(Mono.just(0));

        StepVerifier.create(urlService.disable(shortCode))
                .expectError(ShortUrlNotFoundException.class)
                .verify();
    }
}
