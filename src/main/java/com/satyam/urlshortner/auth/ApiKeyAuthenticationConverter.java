package com.satyam.urlshortner.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyAuthenticationConverter implements ServerAuthenticationConverter {

    public static final String API_KEY_HEADER = "X-API-Key";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.empty();
        }
        return Mono.just(new UsernamePasswordAuthenticationToken(apiKey, apiKey));
    }
}
