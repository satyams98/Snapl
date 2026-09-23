package com.satyam.urlshortner.apikey;

import com.satyam.urlshortner.auth.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    public Flux<ApiKeyResponse> list() {
        return CurrentUser.get().flatMapMany(apiKeyService::list);
    }

    @PostMapping
    public Mono<ApiKeyCreatedResponse> create(@Valid @RequestBody CreateApiKeyRequest request) {
        return CurrentUser.get().flatMap(principal -> apiKeyService.create(principal, request));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> revoke(@PathVariable Long id) {
        return CurrentUser.get()
                .flatMap(principal -> apiKeyService.revoke(principal, id))
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
