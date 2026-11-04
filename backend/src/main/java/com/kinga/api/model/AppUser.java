package com.kinga.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

/** A developer account. The handle is what monitors use as their owner. */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;

    @Column(unique = true, nullable = false)
    public String email;

    /** Short unique name, e.g. "amina-nairobi". Stored as Monitor.owner. */
    @Column(unique = true, nullable = false)
    public String handle;

    /** BCrypt hash, never the password itself. */
    @JsonIgnore
    public String passwordHash;

    public Instant createdAt = Instant.now();
}
