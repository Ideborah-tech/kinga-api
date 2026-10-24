package com.kinga.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Body of POST / PUT /api/monitors. */
public record MonitorRequest(
        @NotBlank String name,
        @NotBlank String owner,
        @NotBlank String provider,
        @NotBlank @Pattern(regexp = "^https?://.+", message = "must start with http:// or https://") String url,
        @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE|HEAD", message = "must be GET, POST, PUT, PATCH, DELETE or HEAD") String method,
        String headers,
        String body,
        @Min(100) @Max(599) Integer expectedStatus,
        String expectedFields,
        @Min(15) @Max(3600) Integer intervalSeconds,
        @Min(1000) @Max(30000) Integer timeoutMs,
        @Min(100) @Max(30000) Integer slowThresholdMs,
        Boolean active,
        Long templateId
) {
}
