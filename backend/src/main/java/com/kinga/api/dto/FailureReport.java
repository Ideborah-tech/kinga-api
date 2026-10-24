package com.kinga.api.dto;

import com.kinga.api.model.CheckResult;
import com.kinga.api.model.FailureType;

import java.time.Instant;
import java.util.List;

/**
 * The Dependency Failure Report: what failed, what was expected, what came back,
 * whose problem it probably is, and what to check next.
 */
public record FailureReport(
        Long monitorId,
        String monitorName,
        String provider,
        String providerName,
        String owner,
        Instant generatedAt,
        /* true when the monitor is currently healthy and this describes the last failure */
        boolean resolved,
        /* false when the monitor has never failed */
        boolean hasFailure,
        Instant failingSince,
        int consecutiveFailures,
        FailureType failureType,
        String title,
        Expected expected,
        Actual actual,
        /* THEM, YOU or UNCLEAR */
        String verdict,
        String verdictHeadline,
        String verdictDetail,
        List<String> suggestions,
        ProviderHealth community,
        String docsUrl,
        String statusPageUrl,
        List<CheckResult> recentChecks
) {
    public record Expected(String method, String url, int status, List<String> fields, int timeoutMs) {
    }

    public record Actual(Integer status, long responseTimeMs, List<String> missingFields, String error,
                         String body, Instant checkedAt) {
    }
}
