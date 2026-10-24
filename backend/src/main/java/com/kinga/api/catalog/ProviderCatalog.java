package com.kinga.api.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The African payment providers KingaAPI knows about, loaded from providers.json. */
@Component
public class ProviderCatalog {

    public record Endpoint(String name, String description, String method, String path, String liveUrl,
                           String headersHint, String body, int expectedStatus, List<String> expectedFields,
                           JsonNode sampleResponse) {
    }

    public record Provider(String code, String slug, String name, String company, List<String> countries,
                           String category, String color, String docsUrl, String statusPageUrl,
                           List<Endpoint> endpoints) {
    }

    private final Map<String, Provider> byCode = new LinkedHashMap<>();
    private final Map<String, Provider> bySlug = new LinkedHashMap<>();
    private final String mockBaseUrl;

    public ProviderCatalog(ObjectMapper objectMapper, @Value("${kinga.mock.base-url}") String mockBaseUrl) throws IOException {
        this.mockBaseUrl = mockBaseUrl;
        try (InputStream in = new ClassPathResource("providers.json").getInputStream()) {
            List<Provider> providers = objectMapper.readValue(in, new TypeReference<>() {
            });
            for (Provider p : providers) {
                byCode.put(p.code(), p);
                bySlug.put(p.slug(), p);
            }
        }
    }

    public List<Provider> all() {
        return List.copyOf(byCode.values());
    }

    public List<String> codes() {
        return List.copyOf(byCode.keySet());
    }

    public Optional<Provider> byCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public Optional<Provider> bySlug(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    public String nameOf(String code) {
        Provider p = byCode.get(code);
        return p != null ? p.name() : code;
    }

    /** URL of the built-in mock for this endpoint, with path placeholders filled with demo values. */
    public String mockUrl(Provider provider, Endpoint endpoint) {
        return mockBaseUrl + "/" + provider.slug() + fillPlaceholders(endpoint.path());
    }

    public static String fillPlaceholders(String path) {
        return path.replaceAll("\\{[^}]+}", "kinga-health-check");
    }
}
