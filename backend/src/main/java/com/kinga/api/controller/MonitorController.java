package com.kinga.api.controller;

import com.kinga.api.dto.FailureReport;
import com.kinga.api.dto.MonitorRequest;
import com.kinga.api.dto.MonitorSummary;
import com.kinga.api.model.CheckResult;
import com.kinga.api.model.Monitor;
import com.kinga.api.model.MonitorTemplate;
import com.kinga.api.repository.CheckResultRepository;
import com.kinga.api.service.MonitorService;
import com.kinga.api.service.MonitoringService;
import com.kinga.api.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;
    private final MonitoringService monitoringService;
    private final ReportService reportService;
    private final CheckResultRepository checks;

    public MonitorController(MonitorService monitorService, MonitoringService monitoringService,
                             ReportService reportService, CheckResultRepository checks) {
        this.monitorService = monitorService;
        this.monitoringService = monitoringService;
        this.reportService = reportService;
        this.checks = checks;
    }

    @GetMapping
    public List<MonitorSummary> list(@RequestParam(required = false) String owner) {
        return monitorService.list(owner);
    }

    @GetMapping("/{id}")
    public MonitorSummary get(@PathVariable Long id) {
        return monitorService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Monitor create(@Valid @RequestBody MonitorRequest request) {
        return monitorService.create(request);
    }

    @PutMapping("/{id}")
    public Monitor update(@PathVariable Long id, @Valid @RequestBody MonitorRequest request) {
        return monitorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        monitorService.delete(id);
    }

    @GetMapping("/{id}/checks")
    public List<CheckResult> checks(@PathVariable Long id, @RequestParam(defaultValue = "60") int limit) {
        monitorService.find(id);
        return checks.findByMonitorIdOrderByCheckedAtDesc(id, PageRequest.of(0, Math.min(limit, 500)));
    }

    @PostMapping("/{id}/check")
    public CheckResult checkNow(@PathVariable Long id) {
        return monitoringService.checkNow(id);
    }

    @GetMapping("/{id}/report")
    public FailureReport report(@PathVariable Long id) {
        return reportService.build(id);
    }

    @PostMapping("/{id}/share")
    @ResponseStatus(HttpStatus.CREATED)
    public MonitorTemplate share(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return monitorService.shareAsTemplate(id, body != null ? body.get("description") : null);
    }
}
