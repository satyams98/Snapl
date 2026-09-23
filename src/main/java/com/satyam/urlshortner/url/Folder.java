package com.satyam.urlshortner.url;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("folders")
@Data
@AllArgsConstructor
public class Folder implements Persistable<Long> {

    @Id
    private Long id;

    @Column("org_id")
    private Long orgId;

    @Column("name")
    private String name;

    @Column("created_at")
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
