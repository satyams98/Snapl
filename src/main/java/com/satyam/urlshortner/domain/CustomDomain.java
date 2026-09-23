package com.satyam.urlshortner.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("custom_domains")
@Data
@AllArgsConstructor
public class CustomDomain implements Persistable<Long> {

    @Id
    private Long id;

    @Column("org_id")
    private Long orgId;

    @Column("domain")
    private String domain;

    @Column("verification_token")
    private String verificationToken;

    @Column("verified_at")
    private Instant verifiedAt;

    @Column("created_at")
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
