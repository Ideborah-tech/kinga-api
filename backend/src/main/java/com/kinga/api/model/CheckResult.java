package com.kinga.api.model;

import jakarta.persistence.*;

import java.time.Instant;

/** The outcome of one request to a monitored endpoint. */
@Entity
@Table(name = "check_results", indexes = {
        @Index(name = "idx_check_monitor_time", columnList = "monitorId,checkedAt"),
        @Index(name = "idx_check_provider_time", columnList = "provider,checkedAt")
})
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long monitorId;
    public String provider;
    public String owner;
    public Instant checkedAt = Instant.now();

    public Integer statusCode;
    public long responseTimeMs;
    public boolean success;
    public boolean formatValid;

    @Column(length = 1000)
    public String missingFields;

    @Enumerated(EnumType.STRING)
    public FailureType failureType = FailureType.NONE;

    @Column(length = 1000)
    public String errorMessage;

    @Column(length = 2000)
    public String responseSnippet;

    @Column(length = 1000)
    public String requestUrl;
    public String requestMethod;
}
