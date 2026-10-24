package com.kinga.api.dto;

import com.kinga.api.model.Monitor;

import java.util.List;

/** A monitor plus its last-24-hour numbers, for the dashboard and detail page. */
public record MonitorSummary(
        Monitor monitor,
        String providerName,
        String providerStatus,
        double uptime24h,
        long avgResponseMs24h,
        int checks24h,
        int failures24h,
        List<Long> recentResponseTimes
) {
}
