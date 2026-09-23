package com.satyam.urlshortner.apikey;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("api_keys")
@Data
@AllArgsConstructor
public class ApiKey implements Persistable<Long> {

    @Id
    private Long id;

    @Column("org_id")
    private Long orgId;

    @Column("name")
    private String name;

    @Column("key_prefix")
    private String keyPrefix;

    @Column("hashed_secret")
    private String hashedSecret;

    // Comma-separated ApiKeyScope names; a single column keeps this entity a plain R2DBC row (no join table).
    @Column("scopes")
    private String scopes;

    @Column("created_at")
    private Instant createdAt;

    @Column("last_used_at")
    private Instant lastUsedAt;

    @Column("revoked_at")
    private Instant revokedAt;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
