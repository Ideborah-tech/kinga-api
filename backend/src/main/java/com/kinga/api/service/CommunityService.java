package com.kinga.api.service;

import com.kinga.api.catalog.ProviderCatalog;
import com.kinga.api.dto.ProviderHealth;
import com.kinga.api.model.FailureType;
import com.kinga.api.model.Monitor;
import com.kinga.api.model.MonitorStatus;
import com.kinga.api.model.ProviderIncident;
import com.kinga.api.repository.MonitorRepository;
import com.kinga.api.repository.ProviderIncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Looks at every developer's monitors for the same provider. When many of them fail at the
 * same time, it opens a provider incident: "it's them, not you".
 */
@Service
public class CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityService.class);

    private final MonitorRepository monitors;
    private final ProviderIncidentRepository incidents;
    private final ProviderCatalog catalog;

    @Value("${kinga.community.min-monitors}")
    private int minMonitors;
    @Value("${kinga.community.failure-ratio}")
    private double failureRatio;
    @Value("${kinga.community.window-minutes}")
    private int windowMinutes;

    public CommunityService(MonitorRepository monitors, ProviderIncidentRepository incidents, ProviderCatalog catalog) {
        this.monitors = monitors;
        this.incidents = incidents;
        this.catalog = catalog;
    }

    /** Called after every check. Synchronized so two parallel checks never open two incidents. */
    public synchronized void evaluate(String provider) {
        ProviderHealth health = health(provider);
        ProviderIncident active = incidents.findFirstByProviderAndActiveTrue(provider).orElse(null);
        boolean widespread = health.totalMonitors() >= minMonitors
                && health.failingMonitors() >= Math.max(2, Math.ceil(health.totalMonitors() * failureRatio));

        if (widespread) {
            ProviderIncident incident = active != null ? active : new ProviderIncident();
            incident.provider = provider;
            incident.affectedMonitors = health.failingMonitors();
            incident.totalMonitors = health.totalMonitors();
            incident.affectedDevelopers = health.affectedDevelopers();
            incident.dominantFailure = health.dominantFailure();
            incident.summary = String.format("%d of %d monitors from %d developers are failing on %s (%s).",
                    health.failingMonitors(), health.totalMonitors(), health.affectedDevelopers(),
                    catalog.nameOf(provider), describe(health.dominantFailure()));
            if (active == null) log.info("Provider incident opened: {}", incident.summary);
            incidents.save(incident);
        } else if (active != null && health.failingMonitors() == 0) {
            active.active = false;
            active.resolvedAt = Instant.now();
            incidents.save(active);
            log.info("Provider incident resolved for {}", provider);
        }
    }

    public ProviderHealth health(String provider) {
        List<Monitor> all = monitors.findByProviderAndActiveTrue(provider);
        Instant windowStart = Instant.now().minusSeconds(windowMinutes * 60L);
        List<Monitor> recent = all.stream()
                .filter(m -> m.lastCheckedAt != null && m.lastCheckedAt.isAfter(windowStart))
                .toList();
        List<Monitor> failing = recent.stream().filter(m -> m.lastStatus == MonitorStatus.DOWN).toList();
        long degraded = recent.stream().filter(m -> m.lastStatus == MonitorStatus.DEGRADED).count();

        FailureType dominant = failing.stream()
                .collect(Collectors.groupingBy(m -> m.lastFailureType, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(FailureType.NONE);

        double avgResponse = recent.stream()
                .filter(m -> m.lastResponseTimeMs != null)
                .mapToLong(m -> m.lastResponseTimeMs)
                .average().orElse(0);

        ProviderIncident incident = incidents.findFirstByProviderAndActiveTrue(provider).orElse(null);

        String status;
        if (recent.isEmpty()) status = "UNKNOWN";
        else if (incident != null) status = "OUTAGE";
        else if (!failing.isEmpty() || degraded * 3 >= recent.size()) status = "DEGRADED";
        else status = "OPERATIONAL";

        return new ProviderHealth(
                provider,
                catalog.nameOf(provider),
                status,
                all.size(),
                failing.size(),
                (int) degraded,
                distinctOwners(all),
                distinctOwners(failing),
                Math.round(avgResponse),
                dominant,
                incident);
    }

    public List<ProviderHealth> allHealth() {
        return catalog.codes().stream()
                .map(this::health)
                .sorted(Comparator.comparing(ProviderHealth::name))
                .toList();
    }

    private static int distinctOwners(List<Monitor> list) {
        return (int) list.stream().map(m -> m.owner).distinct().count();
    }

    static String describe(FailureType type) {
        return switch (type) {
            case SERVER_ERROR -> "server errors";
            case TIMEOUT -> "timeouts";
            case CONNECTION -> "connection failures";
            case AUTH -> "authentication errors";
            case RATE_LIMIT -> "rate limiting";
            case BAD_FORMAT -> "unexpected response format";
            case UNEXPECTED_STATUS -> "unexpected status codes";
            case SLOW -> "slow responses";
            case NONE -> "no errors";
        };
    }
}
