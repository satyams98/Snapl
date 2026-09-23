package com.satyam.urlshortner.url;

import com.satyam.urlshortner.analytics.ClickEventPublisher;
import com.satyam.urlshortner.auth.AuthPrincipal;
import com.satyam.urlshortner.auth.BearerTokenAuthenticationConverter;
import com.satyam.urlshortner.auth.JwtAuthenticationManager;
import com.satyam.urlshortner.auth.JwtService;
import com.satyam.urlshortner.auth.SecurityConfig;
import com.satyam.urlshortner.org.Role;
import com.satyam.urlshortner.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// SecurityConfig is imported (rather than mocking the security context) so requests are
// authenticated through the real JWT filter chain, exercising it the same way production traffic does.
@WebFluxTest(UrlController.class)
@Import({SecurityConfig.class, JwtAuthenticationManager.class, BearerTokenAuthenticationConverter.class, JwtService.class})
class UrlControllerTest {

    private static final AuthPrincipal PRINCIPAL = new AuthPrincipal(1L, "owner@acme.test", 1L, Role.OWNER);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UrlService urlService;

    @MockitoBean
    private ClickEventPublisher clickEventPublisher;

    // RateLimitingWebFilter is a WebFilter, so @WebFluxTest auto-detects and wires it in;
    // its TokenBucketRateLimiter dependency must be mocked to satisfy the slice context.
    @MockitoBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    private WebTestClient authenticatedClient;

    @BeforeEach
    void allowAllRequests() {
        when(tokenBucketRateLimiter.tryConsume(anyString())).thenReturn(Mono.just(true));
        String token = jwtService.issueAccessToken(PRINCIPAL);
        authenticatedClient = webTestClient.mutate().defaultHeader("Authorization", "Bearer " + token).build();
    }

    @Test
    void shortenReturnsOkWithBody() {
        ShortenResponse response = new ShortenResponse("abc123", "http://localhost:8080/abc123", "https://example.com");
        when(urlService.shorten(anyString(), any(), eq(1L))).thenReturn(Mono.just(response));

        authenticatedClient.post().uri("/shorten")
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
        authenticatedClient.post().uri("/shorten")
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
        when(urlService.disable("abc123", 1L)).thenReturn(Mono.empty());

        authenticatedClient.delete().uri("/abc123")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void disableReturns404WhenCodeMissing() {
        when(urlService.disable("missing", 1L)).thenReturn(Mono.error(new ShortUrlNotFoundException("missing")));

        authenticatedClient.delete().uri("/missing")
                .exchange()
                .expectStatus().isNotFound();
    }
}
