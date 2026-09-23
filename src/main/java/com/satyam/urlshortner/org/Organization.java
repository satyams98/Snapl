package com.satyam.urlshortner.org;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("organizations")
@Data
@AllArgsConstructor
public class Organization implements Persistable<Long> {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("slug")
    private String slug;

    @Column("created_at")
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
