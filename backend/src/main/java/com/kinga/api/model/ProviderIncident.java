package com.kinga.api.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Opened when many developers see the same provider failing at the same time: "it's them, not you". */
@Entity
@Table(name = "provider_incidents")
public class ProviderIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String provider;
    public Instant startedAt = Instant.now();
    public Instant resolvedAt;
    public boolean active = true;

    public int affectedMonitors;
    public int totalMonitors;
    public int affectedDevelopers;

    @Enumerated(EnumType.STRING)
    public FailureType dominantFailure;

    @Column(length = 1000)
    public String summary;
}
