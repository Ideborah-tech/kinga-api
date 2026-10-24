package com.kinga.api.dto;

import com.kinga.api.model.FailureType;
import com.kinga.api.model.ProviderIncident;

/** Live status of one provider, built from every developer's monitors. */
public record ProviderHealth(
        String code,
        String name,
        /* OPERATIONAL, DEGRADED, OUTAGE or UNKNOWN */
        String status,
        int totalMonitors,
        int failingMonitors,
        int degradedMonitors,
        int developers,
        int affectedDevelopers,
        long avgResponseMs,
        FailureType dominantFailure,
        ProviderIncident activeIncident
) {
}
