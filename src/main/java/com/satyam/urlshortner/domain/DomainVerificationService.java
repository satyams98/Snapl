package com.satyam.urlshortner.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DomainVerificationService {

    static final String TXT_VALUE_PREFIX = "urlshortener-verify=";

    private final CustomDomainRepository customDomainRepository;
    private final DnsTxtRecordLookup dnsTxtRecordLookup;

    public Mono<CustomDomain> verify(Long orgId, Long domainId) {
        return customDomainRepository.findByOrgIdAndId(orgId, domainId)
                .switchIfEmpty(Mono.error(new DomainNotFoundException(domainId)))
                .flatMap(domain -> hasMatchingTxtRecord(domain)
                        .flatMap(matched -> matched
                                ? customDomainRepository.markVerified(domain.getId(), Instant.now())
                                        .then(customDomainRepository.findById(domain.getId()))
                                : Mono.error(new DomainVerificationFailedException(domain.getDomain()))));
    }

    private Mono<Boolean> hasMatchingTxtRecord(CustomDomain domain) {
        String expected = TXT_VALUE_PREFIX + domain.getVerificationToken();
        return dnsTxtRecordLookup.lookupTxtRecords(domain.getDomain())
                .any(record -> record.trim().equalsIgnoreCase(expected));
    }
}
