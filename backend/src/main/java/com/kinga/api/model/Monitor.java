package com.kinga.api.model;

import jakarta.persistence.*;

import java.time.Instant;

/** An API endpoint that a developer wants KingaAPI to watch. */
@Entity
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;
    /** Developer who owns the monitor (no login for the hackathon, just a handle). */
    public String owner;
    /** Provider code from providers.json, e.g. MPESA, MTN_MOMO. */
    public String provider;

    @Column(length = 1000)
    public String url;
    public String method = "GET";

    /** One "Header-Name: value" per line. */
    @Column(length = 4000)
    public String headers;

    @Column(length = 4000)
    public String body;

    public int expectedStatus = 200;
    /** Comma separated JSON paths that must exist in the response, e.g. "access_token,data.0.code". */
    @Column(length = 1000)
    public String expectedFields;

    public int intervalSeconds = 60;
    public int timeoutMs = 8000;
    public int slowThresholdMs = 2000;
    public boolean active = true;

    @Enumerated(EnumType.STRING)
    public MonitorStatus lastStatus = MonitorStatus.UNKNOWN;
    @Enumerated(EnumType.STRING)
    public FailureType lastFailureType = FailureType.NONE;
    public Instant lastCheckedAt;
    public Long lastResponseTimeMs;
    public Integer lastStatusCode;
    public int consecutiveFailures;

    public Long templateId;
    public Instant createdAt = Instant.now();
}
