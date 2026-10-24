package com.kinga.api.service;

import com.kinga.api.catalog.ProviderCatalog;
import com.kinga.api.dto.MonitorRequest;
import com.kinga.api.dto.MonitorSummary;
import com.kinga.api.model.CheckResult;
import com.kinga.api.model.Monitor;
import com.kinga.api.model.MonitorStatus;
import com.kinga.api.model.MonitorTemplate;
import com.kinga.api.repository.CheckResultRepository;
import com.kinga.api.repository.MonitorRepository;
import com.kinga.api.repository.MonitorTemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Create, edit and list monitors. */
@Service
public class MonitorService {

    private final MonitorRepository monitors;
    private final CheckResultRepository checks;
    private final MonitorTemplateRepository templates;
    private final CommunityService community;
    private final ProviderCatalog catalog;

    @Value("${kinga.check.default-interval-seconds}")
    private int defaultInterval;
    @Value("${kinga.check.timeout-ms}")
    private int defaultTimeout;

    public MonitorService(MonitorRepository monitors, CheckResultRepository checks, MonitorTemplateRepository templates,
                          CommunityService community, ProviderCatalog catalog) {
        this.monitors = monitors;
        this.checks = checks;
        this.templates = templates;
        this.community = community;
        this.catalog = catalog;
    }

    public List<MonitorSummary> list(String owner) {
        List<Monitor> list = owner == null || owner.isBlank()
                ? monitors.findAll()
                : monitors.findByOwnerOrderByIdAsc(owner);
        return list.stream().map(this::summarize).toList();
    }

    public MonitorSummary get(Long id) {
        return summarize(find(id));
    }

    public Monitor find(Long id) {
        return monitors.findById(id).orElseThrow(() -> new NotFoundException("Monitor " + id + " not found"));
    }

    public Monitor create(MonitorRequest request) {
        Monitor monitor = new Monitor();
        apply(monitor, request);
        if (request.templateId() != null) {
            templates.findById(request.templateId()).ifPresent(t -> {
                t.uses++;
                templates.save(t);
            });
        }
        return monitors.save(monitor);
    }

    public Monitor update(Long id, MonitorRequest request) {
        Monitor monitor = find(id);
        String oldUrl = monitor.url;
        apply(monitor, request);
        if (!monitor.url.equals(oldUrl)) {
            monitor.lastStatus = MonitorStatus.UNKNOWN;
        }
        // Check again on the next tick so the change shows up straight away.
        monitor.lastCheckedAt = null;
        return monitors.save(monitor);
    }

    public void delete(Long id) {
        Monitor monitor = find(id);
        checks.deleteByMonitorId(id);
        monitors.delete(monitor);
        community.evaluate(monitor.provider);
    }

    /** Shares a monitor's setup as a community template (without its secrets). */
    public MonitorTemplate shareAsTemplate(Long id, String description) {
        Monitor monitor = find(id);
        MonitorTemplate t = new MonitorTemplate();
        t.provider = monitor.provider;
        t.name = monitor.name;
        t.description = description != null && !description.isBlank() ? description : "Shared by " + monitor.owner;
        t.method = monitor.method;
        t.mockUrl = monitor.url.contains("/mock/") ? monitor.url : null;
        t.liveUrl = monitor.url.contains("/mock/") ? null : monitor.url;
        t.headersHint = redactHeaders(monitor.headers);
        t.body = monitor.body;
        t.expectedStatus = monitor.expectedStatus;
        t.expectedFields = monitor.expectedFields;
        t.author = monitor.owner;
        return templates.save(t);
    }

    /** Keeps header names but replaces values, so nobody shares their real keys by accident. */
    static String redactHeaders(String headers) {
        if (headers == null) return null;
        StringBuilder sb = new StringBuilder();
        for (String[] h : CheckerService.parseHeaders(headers)) {
            boolean secret = h[0].equalsIgnoreCase("Authorization") || h[0].toLowerCase().contains("key")
                    || h[0].toLowerCase().contains("secret") || h[0].toLowerCase().contains("token");
            String value = secret ? "<your " + h[0].toLowerCase() + ">" : h[1];
            if (h[0].equalsIgnoreCase("Authorization") && h[1].contains(" ")) {
                value = h[1].substring(0, h[1].indexOf(' ')) + " <your credentials>";
            }
            sb.append(h[0]).append(": ").append(value).append('\n');
        }
        return sb.toString().trim();
    }

    private void apply(Monitor m, MonitorRequest r) {
        if (catalog.byCode(r.provider()).isEmpty() && !"OTHER".equals(r.provider())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown provider " + r.provider());
        }
        m.name = r.name().trim();
        m.owner = r.owner().trim();
        m.provider = r.provider();
        m.url = r.url().trim();
        m.method = r.method() != null ? r.method() : "GET";
        m.headers = r.headers();
        m.body = r.body();
        m.expectedStatus = r.expectedStatus() != null ? r.expectedStatus() : 200;
        m.expectedFields = r.expectedFields();
        m.intervalSeconds = r.intervalSeconds() != null ? r.intervalSeconds() : defaultInterval;
        m.timeoutMs = r.timeoutMs() != null ? r.timeoutMs() : defaultTimeout;
        m.slowThresholdMs = r.slowThresholdMs() != null ? r.slowThresholdMs() : 2000;
        m.active = r.active() == null || r.active();
        if (r.templateId() != null) m.templateId = r.templateId();
    }

    private MonitorSummary summarize(Monitor m) {
        List<CheckResult> day = checks.findByMonitorIdAndCheckedAtAfterOrderByCheckedAtAsc(
                m.id, Instant.now().minus(Duration.ofHours(24)));
        long ok = day.stream().filter(c -> c.success).count();
        double uptime = day.isEmpty() ? 100.0 : Math.round(ok * 10000.0 / day.size()) / 100.0;
        long avg = Math.round(day.stream().mapToLong(c -> c.responseTimeMs).average().orElse(0));
        List<Long> recent = day.stream()
                .skip(Math.max(0, day.size() - 30))
                .map(c -> c.responseTimeMs)
                .toList();
        return new MonitorSummary(m, catalog.nameOf(m.provider), community.health(m.provider).status(),
                uptime, avg, day.size(), (int) (day.size() - ok), recent);
    }
}
