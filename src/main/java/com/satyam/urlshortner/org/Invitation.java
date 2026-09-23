package com.satyam.urlshortner.org;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("invitations")
@Data
@AllArgsConstructor
public class Invitation implements Persistable<Long> {

    @Id
    private Long id;

    @Column("org_id")
    private Long orgId;

    @Column("email")
    private String email;

    @Column("role")
    private Role role;

    @Column("token")
    private String token;

    @Column("invited_by")
    private Long invitedBy;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("accepted_at")
    private Instant acceptedAt;

    @Column("created_at")
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
