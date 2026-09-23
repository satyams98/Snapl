package com.satyam.urlshortner.url;

import com.satyam.urlshortner.analytics.ClickEventPublisher;
import com.satyam.urlshortner.apikey.ApiKey;
import com.satyam.urlshortner.apikey.ApiKeyRepository;
import com.satyam.urlshortner.auth.ApiKeyAuthenticationConverter;
import com.satyam.urlshortner.auth.ApiKeyAuthenticationManager;
import com.satyam.urlshortner.auth.AuthPrincipal;
import com.satyam.urlshortner.auth.BearerTokenAuthenticationConverter;
import com.satyam.urlshortner.auth.JwtAuthenticationManager;
import com.satyam.urlshortner.auth.JwtService;
import com.satyam.urlshortner.auth.SecurityConfig;
import com.satyam.urlshortner.billing.PlanLimitEnforcer;
import com.satyam.urlshortner.domain.CustomDomainRepository;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// SecurityConfig is imported (rather than mocking the security context) so requests are
// authenticated through the real JWT filter chain, exercising it the same way production traffic does.
@WebFluxTest(UrlController.class)
@Import({SecurityConfig.class, JwtAuthenticationManager.class, BearerTokenAuthenticationConverter.class, JwtService.class,
        ApiKeyAuthenticationManager.class, ApiKeyAuthenticationConverter.class})
class UrlControllerTest {

    private static final AuthPrincipal PRINCIPAL = AuthPrincipal.forUser(1L, "owner@acme.test", 1L, Role.OWNER);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UrlService urlService;

    @MockitoBean
    private ClickEventPublisher clickEventPublisher;

    // RateLimitingWebFilter and DomainResolutionFilter are WebFilters, so @WebFluxTest auto-detects and wires
    // them in; their dependencies must be mocked to satisfy the slice context.
    @MockitoBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @MockitoBean
    private CustomDomainRepository customDomainRepository;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private PlanLimitEnforcer planLimitEnforcer;

    private WebTestClient authenticatedClient;

    @BeforeEach
    void allowAllRequests() {
        when(tokenBucketRateLimiter.tryConsume(anyString())).thenReturn(Mono.just(true));
        when(customDomainRepository.findByDomainAndVerifiedAtIsNotNull(anyString())).thenReturn(Mono.empty());
        when(planLimitEnforcer.checkCanCreateLink(anyLong())).thenReturn(Mono.empty());
        String token = jwtService.issueAccessToken(PRINCIPAL);
        authenticatedClient = webTestClient.mutate().defaultHeader("Authorization", "Bearer " + token).build();
    }

    @Test
    void shortenReturnsOkWithBody() {
        ShortenResponse response = new ShortenResponse("abc123", "http://localhost:8080/abc123", "https://example.com");
        when(urlService.shorten(anyString(), any(), eq(1L), any(), any())).thenReturn(Mono.just(response));

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
        when(urlService.resolve("abc123", null)).thenReturn(Mono.just(new ResolvedLink("https://example.com", false)));

        webTestClient.get().uri("/abc123")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals("Location", "https://example.com");

        verify(clickEventPublisher).publish(any());
    }

    @Test
    void redirectForPasswordProtectedLinkGoesToUnlockPageWithoutPublishingClick() {
        when(urlService.resolve("locked1", null)).thenReturn(Mono.just(new ResolvedLink("https://example.com", true)));

        webTestClient.get().uri("/locked1")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals("Location", "http://localhost:5173/unlock/locked1");

        verifyNoInteractions(clickEventPublisher);
    }

    @Test
    void redirectReturns404WhenNotFound() {
        when(urlService.resolve("missing", null)).thenReturn(Mono.empty());

        webTestClient.get().uri("/missing")
                .exchange()
                .expectStatus().isNotFound();

        verifyNoInteractions(clickEventPublisher);
    }

    @Test
    void unlockReturnsLongUrlAndPublishesClickOnSuccess() {
        when(urlService.unlock("locked1", "secret")).thenReturn(Mono.just("https://example.com"));

        webTestClient.post().uri("/locked1/unlock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"password":"secret"}""")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.longUrl").isEqualTo("https://example.com");

        verify(clickEventPublisher).publish(any());
    }

    @Test
    void unlockReturns401ForWrongPassword() {
        when(urlService.unlock("locked1", "wrong")).thenReturn(Mono.error(new WrongPasswordException("locked1")));

        webTestClient.post().uri("/locked1/unlock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"password":"wrong"}""")
                .exchange()
                .expectStatus().isUnauthorized();
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

    @Test
    void shortenRejectsReadOnlyApiKey() {
        WebTestClient readOnlyClient = readOnlyApiKeyClient();

        readOnlyClient.post().uri("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"longUrl":"https://example.com"}""")
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(urlService);
    }

    @Test
    void disableRejectsReadOnlyApiKey() {
        WebTestClient readOnlyClient = readOnlyApiKeyClient();

        readOnlyClient.delete().uri("/abc123")
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(urlService);
    }

    private WebTestClient readOnlyApiKeyClient() {
        ApiKey readOnlyKey = new ApiKey(1L, 1L, "Read only", "usk_abc123", UrlHasher.sha256Hex("usk_read-only-raw"),
                "READ", Instant.now(), null, null);
        when(apiKeyRepository.findByHashedSecretAndRevokedAtIsNull(UrlHasher.sha256Hex("usk_read-only-raw")))
                .thenReturn(Mono.just(readOnlyKey));
        when(apiKeyRepository.touchLastUsed(eq(1L), any(Instant.class))).thenReturn(Mono.just(1));
        return webTestClient.mutate().defaultHeader("X-API-Key", "usk_read-only-raw").build();
    }
}
