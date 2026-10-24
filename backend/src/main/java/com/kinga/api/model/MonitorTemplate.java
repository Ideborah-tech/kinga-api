package com.kinga.api.model;

import jakarta.persistence.*;

import java.time.Instant;

/** A ready-made monitoring setup shared by developers, e.g. "M-Pesa OAuth token". */
@Entity
@Table(name = "monitor_templates")
public class MonitorTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String provider;
    public String name;

    @Column(length = 1000)
    public String description;

    public String method = "GET";
    /** Path on the provider, used to build the mock URL. */
    @Column(length = 1000)
    public String path;
    /** URL of the built-in mock provider (works offline, used in the demo). */
    @Column(length = 1000)
    public String mockUrl;
    /** Real sandbox URL of the provider. */
    @Column(length = 1000)
    public String liveUrl;

    @Column(length = 4000)
    public String headersHint;
    @Column(length = 4000)
    public String body;

    public int expectedStatus = 200;
    @Column(length = 1000)
    public String expectedFields;

    public String author;
    public int uses;
    public Instant createdAt = Instant.now();
}
