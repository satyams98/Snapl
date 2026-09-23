package com.satyam.urlshortner.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainVerificationServiceTest {

    @Mock
    private CustomDomainRepository customDomainRepository;

    @Mock
    private DnsTxtRecordLookup dnsTxtRecordLookup;

    private DomainVerificationService service;

    private static final Long ORG_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new DomainVerificationService(customDomainRepository, dnsTxtRecordLookup);
    }

    @Test
    void verifyFailsWhenDomainNotFound() {
        when(customDomainRepository.findByOrgIdAndId(ORG_ID, 9L)).thenReturn(Mono.empty());

        StepVerifier.create(service.verify(ORG_ID, 9L))
                .expectError(DomainNotFoundException.class)
                .verify();
    }

    @Test
    void verifyFailsWhenNoMatchingTxtRecordExists() {
        CustomDomain domain = new CustomDomain(1L, ORG_ID, "go.acme.test", "secret-token", null, Instant.now());
        when(customDomainRepository.findByOrgIdAndId(ORG_ID, 1L)).thenReturn(Mono.just(domain));
        when(dnsTxtRecordLookup.lookupTxtRecords("go.acme.test")).thenReturn(Flux.just("some-other-record"));

        StepVerifier.create(service.verify(ORG_ID, 1L))
                .expectError(DomainVerificationFailedException.class)
                .verify();
    }

    @Test
    void verifySucceedsAndMarksDomainVerifiedWhenTxtRecordMatches() {
        CustomDomain domain = new CustomDomain(1L, ORG_ID, "go.acme.test", "secret-token", null, Instant.now());
        CustomDomain verified = new CustomDomain(1L, ORG_ID, "go.acme.test", "secret-token", Instant.now(), domain.getCreatedAt());
        when(customDomainRepository.findByOrgIdAndId(ORG_ID, 1L)).thenReturn(Mono.just(domain));
        when(dnsTxtRecordLookup.lookupTxtRecords("go.acme.test"))
                .thenReturn(Flux.just("urlshortener-verify=secret-token"));
        when(customDomainRepository.markVerified(eq(1L), any(Instant.class))).thenReturn(Mono.just(1));
        when(customDomainRepository.findById(1L)).thenReturn(Mono.just(verified));

        StepVerifier.create(service.verify(ORG_ID, 1L))
                .assertNext(result -> assertEquals(verified.getVerifiedAt(), result.getVerifiedAt()))
                .verifyComplete();
    }
}
