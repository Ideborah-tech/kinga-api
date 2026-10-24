package com.kinga.api.controller;

import com.kinga.api.model.MonitorTemplate;
import com.kinga.api.repository.MonitorTemplateRepository;
import com.kinga.api.service.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Community library of ready-made monitoring setups. */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    public record TemplateRequest(@NotBlank String provider, @NotBlank String name, String description,
                                  String method, String mockUrl, String liveUrl, String headersHint, String body,
                                  Integer expectedStatus, String expectedFields, @NotBlank String author) {
    }

    private final MonitorTemplateRepository templates;

    public TemplateController(MonitorTemplateRepository templates) {
        this.templates = templates;
    }

    @GetMapping
    public List<MonitorTemplate> list(@RequestParam(required = false) String provider) {
        return provider == null || provider.isBlank()
                ? templates.findAllByOrderByUsesDesc()
                : templates.findByProviderOrderByUsesDesc(provider);
    }

    @GetMapping("/{id}")
    public MonitorTemplate get(@PathVariable Long id) {
        return templates.findById(id).orElseThrow(() -> new NotFoundException("Template " + id + " not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitorTemplate create(@Valid @RequestBody TemplateRequest r) {
        MonitorTemplate t = new MonitorTemplate();
        t.provider = r.provider();
        t.name = r.name();
        t.description = r.description();
        t.method = r.method() != null ? r.method() : "GET";
        t.mockUrl = r.mockUrl();
        t.liveUrl = r.liveUrl();
        t.headersHint = r.headersHint();
        t.body = r.body();
        t.expectedStatus = r.expectedStatus() != null ? r.expectedStatus() : 200;
        t.expectedFields = r.expectedFields();
        t.author = r.author();
        return templates.save(t);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        templates.deleteById(id);
    }
}
