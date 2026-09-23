package com.satyam.urlshortner.url;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("urls")
@Data
@AllArgsConstructor
public class UrlEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Column("short_code")
    private String shortCode;

    @Column("long_url")
    private String longUrl;

    @Column("long_url_hash")
    private String longUrlHash;

    @Column("created_at")
    private Instant createdAt;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("is_custom_slug")
    private boolean customSlug;

    @Column("disabled_at")
    private Instant disabledAt;

    @Column("org_id")
    private Long orgId;

    @Column("folder_id")
    private Long folderId;

    @Override
    public boolean isNew() {
        return true; // we only ever construct new rows this weekend — no update path exists yet
    }
}
