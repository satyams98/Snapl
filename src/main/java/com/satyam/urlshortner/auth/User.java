package com.satyam.urlshortner.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("users")
@Data
@AllArgsConstructor
public class User implements Persistable<Long> {

    @Id
    private Long id;

    @Column("email")
    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("name")
    private String name;

    @Column("created_at")
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
