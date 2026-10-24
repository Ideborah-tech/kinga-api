package com.kinga.api.controller;

import com.kinga.api.dto.MonitorSummary;
import com.kinga.api.model.MonitorStatus;
import com.kinga.api.model.ProviderIncident;
import com.kinga.api.repository.ProviderIncidentRepository;
import com.kinga.api.service.MonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    public record Dashboard(int monitors, int up, int degraded, int down, int unknown, double avgUptime24h,
                            int checks24h, int failures24h, List<ProviderIncident> incidentsAffectingYou,
                            List<ProviderIncident> activeIncidents) {
    }

    private final MonitorService monitorService;
    private final ProviderIncidentRepository incidents;

    public DashboardController(MonitorService monitorService, ProviderIncidentRepository incidents) {
        this.monitorService = monitorService;
        this.incidents = incidents;
    }

    @GetMapping
    public Dashboard dashboard(@RequestParam String owner) {
        List<MonitorSummary> mine = monitorService.list(owner);
        List<ProviderIncident> active = incidents.findByActiveTrueOrderByStartedAtDesc();
        List<String> myProviders = mine.stream().map(s -> s.monitor().provider).distinct().toList();

        return new Dashboard(
                mine.size(),
                count(mine, MonitorStatus.UP),
                count(mine, MonitorStatus.DEGRADED),
                count(mine, MonitorStatus.DOWN),
                count(mine, MonitorStatus.UNKNOWN),
                mine.isEmpty() ? 100 : Math.round(mine.stream().mapToDouble(MonitorSummary::uptime24h).average().orElse(100) * 100) / 100.0,
                mine.stream().mapToInt(MonitorSummary::checks24h).sum(),
                mine.stream().mapToInt(MonitorSummary::failures24h).sum(),
                active.stream().filter(i -> myProviders.contains(i.provider)).toList(),
                active);
    }

    private static int count(List<MonitorSummary> list, MonitorStatus status) {
        return (int) list.stream().filter(s -> s.monitor().lastStatus == status).count();
    }
}
