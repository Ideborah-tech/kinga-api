package com.kinga.api.controller;

import com.kinga.api.catalog.ProviderCatalog;
import com.kinga.api.dto.ProviderHealth;
import com.kinga.api.model.CheckResult;
import com.kinga.api.model.ProviderIncident;
import com.kinga.api.repository.ProviderIncidentRepository;
import com.kinga.api.service.CommunityService;
import com.kinga.api.service.MonitoringService;
import com.kinga.api.service.NotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProviderController {

    /** Provider info plus live health, as one object for the frontend. */
    public record ProviderView(ProviderCatalog.Provider info, ProviderHealth health) {
    }

    private final ProviderCatalog catalog;
    private final CommunityService community;
    private final MonitoringService monitoring;
    private final ProviderIncidentRepository incidents;

    public ProviderController(ProviderCatalog catalog, CommunityService community, MonitoringService monitoring,
                              ProviderIncidentRepository incidents) {
        this.catalog = catalog;
        this.community = community;
        this.monitoring = monitoring;
        this.incidents = incidents;
    }

    @GetMapping("/providers")
    public List<ProviderView> providers() {
        return catalog.all().stream()
                .map(p -> new ProviderView(p, community.health(p.code())))
                .toList();
    }

    @GetMapping("/providers/{code}")
    public ProviderView provider(@PathVariable String code) {
        ProviderCatalog.Provider p = catalog.byCode(code)
                .orElseThrow(() -> new NotFoundException("Provider " + code + " not found"));
        return new ProviderView(p, community.health(code));
    }

    @PostMapping("/providers/{code}/check-now")
    public List<CheckResult> checkNow(@PathVariable String code) {
        return monitoring.checkProviderNow(code);
    }

    @GetMapping("/incidents")
    public List<ProviderIncident> incidents(@RequestParam(required = false) Boolean active,
                                            @RequestParam(required = false) String provider) {
        if (Boolean.TRUE.equals(active)) return incidents.findByActiveTrueOrderByStartedAtDesc();
        if (provider != null) return incidents.findTop20ByProviderOrderByStartedAtDesc(provider);
        return incidents.findTop50ByOrderByStartedAtDesc();
    }
}
