package com.satyam.urlshortner.ratelimit;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingWebFilter implements WebFilter {

    private static final String LIMITED_PATH = "/shorten";

    private final TokenBucketRateLimiter rateLimiter;

    public RateLimitingWebFilter(TokenBucketRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!LIMITED_PATH.equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        return rateLimiter.tryConsume(clientKey(exchange))
                .flatMap(allowed -> allowed ? chain.filter(exchange) : reject(exchange));
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Retry-After", "1");
        return exchange.getResponse().setComplete();
    }

    private String clientKey(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";
    }
}
