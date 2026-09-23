package com.satyam.urlshortner.domain;

import com.satyam.urlshortner.auth.CurrentUser;
import com.satyam.urlshortner.billing.PlanLimitEnforcer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/domains")
@RequiredArgsConstructor
public class DomainController {

    private final CustomDomainRepository customDomainRepository;
    private final DomainVerificationService domainVerificationService;
    private final PlanLimitEnforcer planLimitEnforcer;

    @GetMapping
    public Flux<DomainResponse> list() {
        return CurrentUser.get()
                .flatMapMany(principal -> customDomainRepository.findByOrgIdOrderByCreatedAtDesc(principal.orgId()))
                .map(DomainResponse::from);
    }

    @PostMapping
    public Mono<DomainResponse> create(@Valid @RequestBody CreateDomainRequest request) {
        return CurrentUser.get()
                .flatMap(principal -> planLimitEnforcer.checkCanAddCustomDomain(principal.orgId())
                        .then(Mono.defer(() -> customDomainRepository.existsByDomain(request.domain())))
                        .flatMap(exists -> exists
                                ? Mono.error(new DomainAlreadyExistsException(request.domain()))
                                : customDomainRepository.save(new CustomDomain(null, principal.orgId(), request.domain(),
                                        generateToken(), null, Instant.now()))))
                .map(DomainResponse::from);
    }

    @PostMapping("/{id}/verify")
    public Mono<DomainResponse> verify(@PathVariable Long id) {
        return CurrentUser.get()
                .flatMap(principal -> domainVerificationService.verify(principal.orgId(), id))
                .map(DomainResponse::from);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return CurrentUser.get()
                .flatMap(principal -> customDomainRepository.findByOrgIdAndId(principal.orgId(), id)
                        .switchIfEmpty(Mono.error(new DomainNotFoundException(id)))
                        .flatMap(customDomainRepository::delete))
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
