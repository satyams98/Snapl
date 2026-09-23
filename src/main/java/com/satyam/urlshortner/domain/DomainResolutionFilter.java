package com.satyam.urlshortner.domain;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

// Resolves which org (if any) owns the custom domain a request arrived on, so the redirect
// handler can restrict a branded domain to serving only that org's own links.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DomainResolutionFilter implements WebFilter {

    public static final String RESOLVED_ORG_ID_ATTRIBUTE = "customDomainOrgId";

    private final CustomDomainRepository customDomainRepository;

    public DomainResolutionFilter(CustomDomainRepository customDomainRepository) {
        this.customDomainRepository = customDomainRepository;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String host = hostWithoutPort(exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST));
        if (host == null) {
            return chain.filter(exchange);
        }
        return customDomainRepository.findByDomainAndVerifiedAtIsNotNull(host)
                .doOnNext(domain -> exchange.getAttributes().put(RESOLVED_ORG_ID_ATTRIBUTE, domain.getOrgId()))
                .then(Mono.defer(() -> chain.filter(exchange)));
    }

    private String hostWithoutPort(String hostHeader) {
        if (hostHeader == null) {
            return null;
        }
        int colon = hostHeader.indexOf(':');
        return colon >= 0 ? hostHeader.substring(0, colon) : hostHeader;
    }
}
