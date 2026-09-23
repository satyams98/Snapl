package com.satyam.urlshortner.ratelimit;

import com.satyam.urlshortner.url.UrlHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingWebFilterTest {

    @Mock
    private TokenBucketRateLimiter rateLimiter;

    @Mock
    private WebFilterChain chain;

    private RateLimitingWebFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingWebFilter(rateLimiter);
    }

    @Test
    void allowsRequestWhenTokenAvailable() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/shorten"));
        when(rateLimiter.tryConsume(anyString())).thenReturn(Mono.just(true));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void rejectsRequestWithTooManyRequestsWhenBucketEmpty() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/shorten"));
        when(rateLimiter.tryConsume(anyString())).thenReturn(Mono.just(false));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals(429, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(exchange);
    }

    @Test
    void bypassesRateLimitingForUnrelatedPaths() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/abc123"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void keysByHashedApiKeyRatherThanIpWhenHeaderPresent() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/shorten").header("X-API-Key", "usk_some-raw-key"));
        when(rateLimiter.tryConsume(anyString())).thenReturn(Mono.just(true));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter).tryConsume(keyCaptor.capture());
        assertEquals("apikey:" + UrlHasher.sha256Hex("usk_some-raw-key"), keyCaptor.getValue());
    }
}
