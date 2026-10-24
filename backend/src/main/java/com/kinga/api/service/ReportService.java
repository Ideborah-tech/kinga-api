package com.kinga.api.service;

import com.kinga.api.catalog.ProviderCatalog;
import com.kinga.api.dto.FailureReport;
import com.kinga.api.dto.ProviderHealth;
import com.kinga.api.model.CheckResult;
import com.kinga.api.model.FailureType;
import com.kinga.api.model.Monitor;
import com.kinga.api.model.MonitorStatus;
import com.kinga.api.repository.CheckResultRepository;
import com.kinga.api.repository.MonitorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Builds the Dependency Failure Report for a monitor. */
@Service
public class ReportService {

    private final MonitorRepository monitors;
    private final CheckResultRepository checks;
    private final CommunityService community;
    private final ProviderCatalog catalog;

    @Value("${kinga.community.min-monitors}")
    private int minMonitors;

    public ReportService(MonitorRepository monitors, CheckResultRepository checks, CommunityService community,
                         ProviderCatalog catalog) {
        this.monitors = monitors;
        this.checks = checks;
        this.community = community;
        this.catalog = catalog;
    }

    public FailureReport build(Long monitorId) {
        Monitor monitor = monitors.findById(monitorId)
                .orElseThrow(() -> new NotFoundException("Monitor " + monitorId + " not found"));
        ProviderHealth health = community.health(monitor.provider);
        var provider = catalog.byCode(monitor.provider).orElse(null);
        String providerName = catalog.nameOf(monitor.provider);
        String docsUrl = provider != null ? provider.docsUrl() : null;
        String statusPageUrl = provider != null ? provider.statusPageUrl() : null;

        List<CheckResult> recent = checks.findByMonitorIdOrderByCheckedAtDesc(monitorId, PageRequest.of(0, 15));
        CheckResult failed = checks.findFirstByMonitorIdAndSuccessFalseOrderByCheckedAtDesc(monitorId).orElse(null);

        FailureReport.Expected expected = new FailureReport.Expected(monitor.method, monitor.url,
                monitor.expectedStatus, CheckerService.splitFields(monitor.expectedFields), monitor.timeoutMs);

        if (failed == null) {
            return new FailureReport(monitor.id, monitor.name, monitor.provider, providerName, monitor.owner,
                    Instant.now(), true, false, null, 0, FailureType.NONE, "No failures recorded",
                    expected, null, "NONE", "All good.",
                    "This endpoint has not failed yet. KingaAPI checks it every " + monitor.intervalSeconds + " seconds.",
                    List.of(), health, docsUrl, statusPageUrl, recent);
        }

        boolean resolved = monitor.lastStatus != MonitorStatus.DOWN;
        Instant failingSince = checks.findFirstByMonitorIdAndSuccessTrueOrderByCheckedAtDesc(monitorId)
                .map(lastOk -> firstFailureAfter(recent, lastOk.checkedAt))
                .orElse(failed.checkedAt);

        FailureReport.Actual actual = new FailureReport.Actual(failed.statusCode, failed.responseTimeMs,
                CheckerService.splitFields(failed.missingFields), failed.errorMessage, failed.responseSnippet,
                failed.checkedAt);

        Verdict verdict = verdict(monitor, failed.failureType, health, providerName, statusPageUrl);

        return new FailureReport(monitor.id, monitor.name, monitor.provider, providerName, monitor.owner,
                Instant.now(), resolved, true, failingSince, monitor.consecutiveFailures, failed.failureType,
                titleOf(failed.failureType), expected, actual,
                verdict.code, verdict.headline, verdict.detail,
                suggestions(monitor, failed, providerName, statusPageUrl),
                health, docsUrl, statusPageUrl, recent);
    }

    private record Verdict(String code, String headline, String detail) {
    }

    private Verdict verdict(Monitor monitor, FailureType type, ProviderHealth health, String providerName,
                            String statusPageUrl) {
        if (health.activeIncident() != null) {
            return new Verdict("THEM", "It's them, not you.",
                    health.activeIncident().summary + " Your code is probably fine. Follow " + providerName
                            + " updates" + (statusPageUrl != null ? " at " + statusPageUrl : "")
                            + " and retry once they recover.");
        }
        int others = health.totalMonitors() - 1;
        int othersFailing = Math.max(0, health.failingMonitors() - (monitor.lastStatus == MonitorStatus.DOWN ? 1 : 0));
        int othersDevelopers = Math.max(0, health.developers() - 1);
        if (others + 1 < minMonitors) {
            return new Verdict("UNCLEAR", "Not enough shared data yet.",
                    "Only " + health.totalMonitors() + " monitor(s) watch " + providerName
                            + " right now, so KingaAPI cannot compare with other developers. Use the checks below.");
        }
        if (othersFailing == 0) {
            String cause = type.isProviderSide()
                    ? "It may be your account, region or network path to " + providerName + "."
                    : "The problem is most likely your credentials or your request.";
            return new Verdict("YOU", "Probably your setup, not " + providerName + ".",
                    others + " other monitor(s) from " + othersDevelopers + " other developer(s) on " + providerName
                            + " are healthy right now. " + cause);
        }
        return new Verdict("UNCLEAR", "Some other developers are affected too.",
                othersFailing + " of " + others + " other monitor(s) on " + providerName
                        + " are also failing. That is not yet enough to confirm a provider outage, so KingaAPI keeps watching.");
    }

    static String titleOf(FailureType type) {
        return switch (type) {
            case AUTH -> "Authentication failed";
            case RATE_LIMIT -> "Rate limit reached";
            case SERVER_ERROR -> "Provider server error";
            case TIMEOUT -> "Request timed out";
            case CONNECTION -> "Could not connect";
            case BAD_FORMAT -> "Unexpected response format";
            case UNEXPECTED_STATUS -> "Unexpected status code";
            case SLOW -> "Slow responses";
            case NONE -> "No failure";
        };
    }

    private static List<String> suggestions(Monitor monitor, CheckResult failed, String provider, String statusPageUrl) {
        List<String> s = new ArrayList<>();
        String statusPage = statusPageUrl != null ? " (" + statusPageUrl + ")" : "";
        switch (failed.failureType) {
            case AUTH -> {
                s.add("Check that your key and secret belong to the right environment: sandbox keys do not work in production, and production keys do not work in sandbox.");
                s.add("Access tokens expire (M-Pesa tokens last one hour). Make sure your app refreshes the token before it expires.");
                s.add("Check the Authorization header format: \"Bearer <token>\" for API calls, \"Basic <base64(key:secret)>\" for token calls.");
                s.add("Check whether the key was rotated or revoked in your " + provider + " dashboard.");
            }
            case RATE_LIMIT -> {
                s.add("You are sending too many requests. Respect the Retry-After header before retrying.");
                s.add("Cache access tokens and reuse them instead of requesting a new token for every call.");
                s.add("Add retries with exponential backoff to your payment calls.");
                s.add("If your traffic is legitimate, ask " + provider + " for a higher rate limit.");
            }
            case SERVER_ERROR -> {
                s.add(provider + " returned HTTP " + failed.statusCode + ", an error on their side. Check their status page" + statusPage + ".");
                s.add("Retry with backoff and queue payments instead of failing them immediately.");
                s.add("Show customers a clear \"payment provider is busy, try again shortly\" message.");
                s.add("If it lasts, contact " + provider + " support and include the request ID from the response body.");
            }
            case TIMEOUT -> {
                s.add(provider + " did not answer within " + monitor.timeoutMs + " ms. Check their status page" + statusPage + ".");
                s.add("Do not double-charge: a timed-out payment may still go through. Check the transaction status before retrying.");
                s.add("Use the provider's callback (webhook) instead of waiting on the request.");
                s.add("Check your own server's outbound network and DNS.");
            }
            case CONNECTION -> {
                s.add("Check the URL and that the host resolves (DNS): " + monitor.url);
                s.add("Some providers only accept requests from whitelisted server IPs. Check that your server's IP is registered.");
                s.add("Check TLS: an outdated certificate store or TLS version can block the connection.");
                s.add("Check firewalls or proxies between your server and " + provider + ".");
            }
            case BAD_FORMAT -> {
                if (failed.missingFields != null && !failed.missingFields.isBlank()) {
                    s.add("These fields your code expects are missing: " + failed.missingFields + ".");
                } else {
                    s.add("The response is not valid JSON. It may be an HTML error page from a proxy or gateway.");
                }
                s.add("Check the " + provider + " changelog for a new API version or renamed fields.");
                s.add("Make your parser tolerant of extra or missing fields so a schema change does not crash checkout.");
                s.add("If the change is intended, update this monitor's expected fields.");
            }
            case UNEXPECTED_STATUS -> {
                s.add("Expected HTTP " + monitor.expectedStatus + " but got " + failed.statusCode + ".");
                if (failed.statusCode != null && failed.statusCode == 404) {
                    s.add("404 usually means a wrong path or API version. Compare the URL with the " + provider + " docs.");
                } else if (failed.statusCode != null && failed.statusCode == 400) {
                    s.add("400 usually means the request body is invalid. Check required fields and phone number format (e.g. 2547XXXXXXXX).");
                }
                s.add("Read the error message in the response body below; providers usually say which field is wrong.");
            }
            case SLOW -> {
                s.add(provider + " is answering, but slower than " + monitor.slowThresholdMs + " ms.");
                s.add("Customers may abandon checkout. Show a loading state and avoid chaining many calls.");
            }
            case NONE -> {
            }
        }
        return s;
    }

    /** Oldest failed check after the last success, from the newest-first list of recent checks. */
    private static Instant firstFailureAfter(List<CheckResult> recentNewestFirst, Instant lastSuccess) {
        Instant first = null;
        for (CheckResult c : recentNewestFirst) {
            if (!c.checkedAt.isAfter(lastSuccess)) break;
            if (!c.success) first = c.checkedAt;
        }
        return first;
    }
}
