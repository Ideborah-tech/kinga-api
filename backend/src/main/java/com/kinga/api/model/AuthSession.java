package com.kinga.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A signed-in browser. The token is sent as "Authorization: Bearer <token>". */
@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    public String token;

    public Long userId;
    public Instant createdAt = Instant.now();
}
