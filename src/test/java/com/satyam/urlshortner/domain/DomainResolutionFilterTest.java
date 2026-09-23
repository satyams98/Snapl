package com.satyam.urlshortner.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainResolutionFilterTest {

    @Mock
    private CustomDomainRepository customDomainRepository;

    @Mock
    private WebFilterChain chain;

    private DomainResolutionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DomainResolutionFilter(customDomainRepository);
    }

    @Test
    void setsResolvedOrgIdWhenHostMatchesVerifiedDomain() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/abc123").header("Host", "go.acme.test"));
        CustomDomain domain = new CustomDomain(1L, 42L, "go.acme.test", "token", Instant.now(), Instant.now());
        when(customDomainRepository.findByDomainAndVerifiedAtIsNotNull("go.acme.test")).thenReturn(Mono.just(domain));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals(Long.valueOf(42L), exchange.getAttribute(DomainResolutionFilter.RESOLVED_ORG_ID_ATTRIBUTE));
        verify(chain).filter(exchange);
    }

    @Test
    void leavesAttributeUnsetWhenHostIsNotACustomDomain() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/abc123").header("Host", "localhost:8080"));
        when(customDomainRepository.findByDomainAndVerifiedAtIsNotNull("localhost")).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertNull(exchange.getAttribute(DomainResolutionFilter.RESOLVED_ORG_ID_ATTRIBUTE));
        verify(chain).filter(exchange);
    }
}
