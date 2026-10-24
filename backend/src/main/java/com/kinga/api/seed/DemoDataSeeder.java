package com.kinga.api.seed;

import com.kinga.api.catalog.ProviderCatalog;
import com.kinga.api.catalog.ProviderCatalog.Endpoint;
import com.kinga.api.catalog.ProviderCatalog.Provider;
import com.kinga.api.model.CheckResult;
import com.kinga.api.model.FailureType;
import com.kinga.api.model.Monitor;
import com.kinga.api.model.MonitorTemplate;
import com.kinga.api.model.ProviderIncident;
import com.kinga.api.repository.CheckResultRepository;
import com.kinga.api.repository.MonitorRepository;
import com.kinga.api.repository.MonitorTemplateRepository;
import com.kinga.api.repository.ProviderIncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Fills an empty database with community templates, monitors from several developers
 * across Africa, and 24 hours of check history including one past MTN MoMo outage.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** The developer using the dashboard in the demo. */
    public static final String DEMO_OWNER = "you";

    /** developer -> list of "PROVIDER_CODE:endpointIndex" */
    private static final Map<String, List<String>> COMMUNITY = Map.of(
            DEMO_OWNER, List.of("MPESA:0", "MPESA:1", "MTN_MOMO:0", "FLUTTERWAVE:0", "PAYSTACK:1"),
            "amina-nairobi", List.of("MPESA:0", "MPESA:1", "AIRTEL_MONEY:0", "FLUTTERWAVE:1"),
            "brian-kisumu", List.of("MPESA:1", "AIRTEL_MONEY:0"),
            "kwame-accra", List.of("MTN_MOMO:1", "PAYSTACK:0", "FLUTTERWAVE:0"),
            "chidi-lagos", List.of("PAYSTACK:0", "PAYSTACK:1", "FLUTTERWAVE:1"),
            "grace-kampala", List.of("MTN_MOMO:0", "MTN_MOMO:1", "AIRTEL_MONEY:0"),
            "awa-abidjan", List.of("ORANGE_MONEY:0", "MTN_MOMO:1"),
            "moussa-dakar", List.of("ORANGE_MONEY:0"),
            "fatou-bamako", List.of("ORANGE_MONEY:0"),
            "zanele-joburg", List.of("PAYSTACK:0", "MPESA:0")
    );

    private static final Map<String, Integer> BASE_LATENCY = Map.of(
            "MPESA", 420, "MTN_MOMO", 610, "AIRTEL_MONEY", 540,
            "ORANGE_MONEY", 680, "FLUTTERWAVE", 310, "PAYSTACK", 260);

    private final ProviderCatalog catalog;
    private final MonitorRepository monitors;
    private final CheckResultRepository checks;
    private final MonitorTemplateRepository templates;
    private final ProviderIncidentRepository incidents;
    private final Random random = new Random(42);

    public DemoDataSeeder(ProviderCatalog catalog, MonitorRepository monitors, CheckResultRepository checks,
                          MonitorTemplateRepository templates, ProviderIncidentRepository incidents) {
        this.catalog = catalog;
        this.monitors = monitors;
        this.checks = checks;
        this.templates = templates;
        this.incidents = incidents;
    }

    @Override
    public void run(String... args) {
        if (monitors.count() > 0) return;
        seedTemplates();
        List<Monitor> created = seedMonitors();
        seedHistory(created);
        log.info("Seeded {} templates, {} monitors and {} checks", templates.count(), monitors.count(), checks.count());
    }

    private void seedTemplates() {
        String[] authors = {"KingaAPI community", "amina-nairobi", "kwame-accra", "chidi-lagos", "grace-kampala"};
        int i = 0;
        for (Provider p : catalog.all()) {
            for (Endpoint e : p.endpoints()) {
                MonitorTemplate t = new MonitorTemplate();
                t.provider = p.code();
                t.name = e.name();
                t.description = e.description();
                t.method = e.method();
                t.path = e.path();
                t.mockUrl = catalog.mockUrl(p, e);
                t.liveUrl = e.liveUrl();
                t.headersHint = e.headersHint();
                t.body = e.body();
                t.expectedStatus = e.expectedStatus();
                t.expectedFields = String.join(",", e.expectedFields());
                t.author = authors[i++ % authors.length];
                t.uses = 8 + random.nextInt(140);
                t.createdAt = Instant.now().minus(Duration.ofDays(3 + random.nextInt(60)));
                templates.save(t);
            }
        }
    }

    private List<Monitor> seedMonitors() {
        List<Monitor> created = new ArrayList<>();
        // Seed the demo owner first so their monitors get the lowest ids.
        List<String> owners = new ArrayList<>(COMMUNITY.keySet());
        owners.sort((a, b) -> a.equals(DEMO_OWNER) ? -1 : b.equals(DEMO_OWNER) ? 1 : a.compareTo(b));
        for (String owner : owners) {
            for (String ref : COMMUNITY.get(owner)) {
                String[] parts = ref.split(":");
                Provider p = catalog.byCode(parts[0]).orElseThrow();
                Endpoint e = p.endpoints().get(Integer.parseInt(parts[1]));

                Monitor m = new Monitor();
                m.owner = owner;
                m.provider = p.code();
                m.name = e.name();
                m.url = catalog.mockUrl(p, e);
                m.method = e.method();
                m.headers = demoHeaders(e.headersHint(), owner);
                m.body = e.body();
                m.expectedStatus = e.expectedStatus();
                m.expectedFields = String.join(",", e.expectedFields());
                m.createdAt = Instant.now().minus(Duration.ofDays(2 + random.nextInt(30)));
                created.add(monitors.save(m));
            }
        }
        return created;
    }

    /** Replaces the placeholders in the template headers with a fake per-developer key. */
    private static String demoHeaders(String hint, String owner) {
        if (hint == null) return null;
        String key = "demo_" + owner.replace('-', '_') + "_key";
        return hint.replaceAll("<[^>]+>", key);
    }

    private void seedHistory(List<Monitor> created) {
        Instant now = Instant.now();
        Instant mtnOutageStart = now.minus(Duration.ofHours(6));
        Instant mtnOutageEnd = mtnOutageStart.plus(Duration.ofMinutes(40));
        Instant paystackSlowStart = now.minus(Duration.ofHours(14));
        Instant paystackSlowEnd = paystackSlowStart.plus(Duration.ofMinutes(30));

        List<CheckResult> batch = new ArrayList<>();
        for (Monitor m : created) {
            int base = BASE_LATENCY.getOrDefault(m.provider, 400);
            for (int minutesAgo = 24 * 60; minutesAgo > 5; minutesAgo -= 10) {
                Instant at = now.minus(Duration.ofMinutes(minutesAgo));
                CheckResult c = new CheckResult();
                c.monitorId = m.id;
                c.provider = m.provider;
                c.owner = m.owner;
                c.checkedAt = at;
                c.requestUrl = m.url;
                c.requestMethod = m.method;
                c.responseTimeMs = Math.max(40, Math.round(base + random.nextGaussian() * base * 0.25));

                boolean inMtnOutage = m.provider.equals("MTN_MOMO") && at.isAfter(mtnOutageStart) && at.isBefore(mtnOutageEnd);
                boolean inPaystackSlow = m.provider.equals("PAYSTACK") && at.isAfter(paystackSlowStart) && at.isBefore(paystackSlowEnd);

                if (inMtnOutage) {
                    c.statusCode = 503;
                    c.failureType = FailureType.SERVER_ERROR;
                    c.errorMessage = "Provider server error (HTTP 503)";
                    c.responseSnippet = "{\"error\":\"Service Unavailable\"}";
                } else if (random.nextDouble() < 0.008) {
                    c.statusCode = null;
                    c.responseTimeMs = m.timeoutMs;
                    c.failureType = FailureType.TIMEOUT;
                    c.errorMessage = "No response within " + m.timeoutMs + " ms";
                } else {
                    c.statusCode = m.expectedStatus;
                    c.success = true;
                    c.formatValid = true;
                    if (inPaystackSlow) {
                        c.responseTimeMs = 2600 + random.nextInt(1200);
                        c.failureType = FailureType.SLOW;
                        c.errorMessage = "Slow response: " + c.responseTimeMs + " ms (threshold " + m.slowThresholdMs + " ms)";
                    }
                }
                batch.add(c);
            }
        }
        checks.saveAll(batch);

        long mtnMonitors = created.stream().filter(m -> m.provider.equals("MTN_MOMO")).count();
        long mtnDevelopers = created.stream().filter(m -> m.provider.equals("MTN_MOMO")).map(m -> m.owner).distinct().count();
        ProviderIncident past = new ProviderIncident();
        past.provider = "MTN_MOMO";
        past.startedAt = mtnOutageStart;
        past.resolvedAt = mtnOutageEnd;
        past.active = false;
        past.affectedMonitors = (int) mtnMonitors;
        past.totalMonitors = (int) mtnMonitors;
        past.affectedDevelopers = (int) mtnDevelopers;
        past.dominantFailure = FailureType.SERVER_ERROR;
        past.summary = String.format("%d of %d monitors from %d developers were failing on MTN MoMo (server errors).",
                mtnMonitors, mtnMonitors, mtnDevelopers);
        incidents.save(past);
    }
}
