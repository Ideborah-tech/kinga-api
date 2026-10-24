package com.kinga.api.service;

import com.kinga.api.model.CheckResult;
import com.kinga.api.model.FailureType;
import com.kinga.api.model.Monitor;
import com.kinga.api.model.MonitorStatus;
import com.kinga.api.repository.CheckResultRepository;
import com.kinga.api.repository.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/** Runs checks on a schedule (every monitor's interval, default one minute) and stores the results. */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final MonitorRepository monitors;
    private final CheckResultRepository checks;
    private final CheckerService checker;
    private final CommunityService community;
    private final ExecutorService checkExecutor;

    /** Monitors with a check currently running, so a slow endpoint is never checked twice at once. */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public MonitoringService(MonitorRepository monitors, CheckResultRepository checks, CheckerService checker,
                             CommunityService community, ExecutorService checkExecutor) {
        this.monitors = monitors;
        this.checks = checks;
        this.checker = checker;
        this.community = community;
        this.checkExecutor = checkExecutor;
    }

    @Scheduled(initialDelay = 3000, fixedDelayString = "${kinga.check.tick-ms}")
    public void runDueChecks() {
        Instant now = Instant.now();
        for (Monitor monitor : monitors.findByActiveTrue()) {
            boolean due = monitor.lastCheckedAt == null
                    || monitor.lastCheckedAt.plusSeconds(monitor.intervalSeconds).isBefore(now);
            if (due) submit(monitor.id);
        }
    }

    /** Checks one monitor right now and returns the result. */
    public CheckResult checkNow(Long monitorId) {
        Monitor monitor = monitors.findById(monitorId)
                .orElseThrow(() -> new NotFoundException("Monitor " + monitorId + " not found"));
        return runCheck(monitor);
    }

    /** Checks every active monitor of a provider in parallel and waits for all of them. */
    public List<CheckResult> checkProviderNow(String provider) {
        List<CompletableFuture<CheckResult>> futures = new ArrayList<>();
        for (Monitor monitor : monitors.findByProviderAndActiveTrue(provider)) {
            futures.add(CompletableFuture.supplyAsync(() -> runCheck(monitor), checkExecutor));
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private void submit(Long monitorId) {
        if (!inFlight.add(monitorId)) return;
        checkExecutor.submit(() -> {
            try {
                monitors.findById(monitorId).ifPresent(this::runCheck);
            } catch (Exception e) {
                log.warn("Check for monitor {} failed unexpectedly", monitorId, e);
            } finally {
                inFlight.remove(monitorId);
            }
        });
    }

    private CheckResult runCheck(Monitor monitor) {
        CheckResult result = checks.save(checker.check(monitor));

        // Re-read so edits made while the request was in flight are not overwritten.
        Monitor current = monitors.findById(monitor.id).orElse(null);
        if (current == null) return result;
        current.lastCheckedAt = result.checkedAt;
        current.lastResponseTimeMs = result.responseTimeMs;
        current.lastStatusCode = result.statusCode;
        current.lastFailureType = result.failureType;
        current.lastStatus = statusOf(result);
        current.consecutiveFailures = result.success ? 0 : current.consecutiveFailures + 1;
        monitors.save(current);

        community.evaluate(current.provider);
        return result;
    }

    static MonitorStatus statusOf(CheckResult result) {
        if (!result.success) return MonitorStatus.DOWN;
        return result.failureType == FailureType.SLOW ? MonitorStatus.DEGRADED : MonitorStatus.UP;
    }
}
