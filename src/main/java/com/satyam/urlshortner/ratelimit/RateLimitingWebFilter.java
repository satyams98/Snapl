package com.satyam.urlshortner.ratelimit;

import com.satyam.urlshortner.auth.ApiKeyAuthenticationConverter;
import com.satyam.urlshortner.url.UrlHasher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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

    private final TokenBucketRateLimiter rateLimiter;

    public RateLimitingWebFilter(TokenBucketRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!isMutatingRequest(exchange)) {
            return chain.filter(exchange);
        }

        return rateLimiter.tryConsume(clientKey(exchange))
                .flatMap(allowed -> allowed ? chain.filter(exchange) : reject(exchange));
    }

    // Redirects (GET) stay unthrottled to preserve ultra-low read latency; only writes are rate limited.
    private boolean isMutatingRequest(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        return HttpMethod.POST.equals(method) || HttpMethod.DELETE.equals(method);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Retry-After", "1");
        return exchange.getResponse().setComplete();
    }

    private String clientKey(ServerWebExchange exchange) {
        // Keyed by the presented API key (hashed) rather than IP when present, so integrations get their
        // own bucket independent of shared egress IPs; validity of the key is checked later by auth.
        String apiKey = exchange.getRequest().getHeaders().getFirst(ApiKeyAuthenticationConverter.API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            return "apikey:" + UrlHasher.sha256Hex(apiKey);
        }
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";
    }
}
