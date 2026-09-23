package com.satyam.urlshortner.domain;

import java.time.Instant;

public record DomainResponse(
        Long id,
        String domain,
        boolean verified,
        String txtRecordName,
        String txtRecordValue,
        Instant createdAt
) {
    static DomainResponse from(CustomDomain domain) {
        return new DomainResponse(
                domain.getId(),
                domain.getDomain(),
                domain.getVerifiedAt() != null,
                domain.getDomain(),
                DomainVerificationService.TXT_VALUE_PREFIX + domain.getVerificationToken(),
                domain.getCreatedAt());
    }
}
