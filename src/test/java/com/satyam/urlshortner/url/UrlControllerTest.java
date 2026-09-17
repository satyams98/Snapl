package com.satyam.urlshortner.url;

import com.satyam.urlshortner.analytics.ClickEventPublisher;
import com.satyam.urlshortner.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UrlService urlService;

    @MockitoBean
    private ClickEventPublisher clickEventPublisher;

    // RateLimitingWebFilter is a WebFilter, so @WebFluxTest auto-detects and wires it in;
    // its TokenBucketRateLimiter dependency must be mocked to satisfy the slice context.
    @MockitoBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @BeforeEach
    void allowAllRequests() {
        when(tokenBucketRateLimiter.tryConsume(anyString())).thenReturn(Mono.just(true));
    }

    @Test
    void shortenReturnsOkWithBody() {
        ShortenResponse response = new ShortenResponse("abc123", "http://localhost:8080/abc123", "https://example.com");
        when(urlService.shorten(anyString(), any())).thenReturn(Mono.just(response));

        webTestClient.post().uri("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"longUrl":"https://example.com"}""")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.shortCode").isEqualTo("abc123");
    }

    @Test
    void shortenRejectsInvalidLongUrl() {
        webTestClient.post().uri("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"longUrl":"not-a-url"}""")
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(urlService);
    }

    @Test
    void redirectReturns302AndPublishesClickEvent() {
        when(urlService.resolve("abc123")).thenReturn(Mono.just("https://example.com"));

        webTestClient.get().uri("/abc123")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals("Location", "https://example.com");

        verify(clickEventPublisher).publish(any());
    }

    @Test
    void redirectReturns404WhenNotFound() {
        when(urlService.resolve("missing")).thenReturn(Mono.empty());

        webTestClient.get().uri("/missing")
                .exchange()
                .expectStatus().isNotFound();

        verifyNoInteractions(clickEventPublisher);
    }

    @Test
    void disableReturnsNoContent() {
        when(urlService.disable("abc123")).thenReturn(Mono.empty());

        webTestClient.delete().uri("/abc123")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void disableReturns404WhenCodeMissing() {
        when(urlService.disable("missing")).thenReturn(Mono.error(new ShortUrlNotFoundException("missing")));

        webTestClient.delete().uri("/missing")
                .exchange()
                .expectStatus().isNotFound();
    }
}
