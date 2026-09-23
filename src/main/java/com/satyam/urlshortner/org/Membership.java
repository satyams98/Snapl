package com.satyam.urlshortner.org;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("memberships")
@Data
@AllArgsConstructor
public class Membership implements Persistable<Long> {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("org_id")
    private Long orgId;

    @Column("role")
    private Role role;

    @Column("created_at")
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
