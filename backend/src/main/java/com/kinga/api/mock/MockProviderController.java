package com.kinga.api.mock;

import com.kinga.api.catalog.ProviderCatalog;
import com.kinga.api.catalog.ProviderCatalog.Endpoint;
import com.kinga.api.catalog.ProviderCatalog.Provider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Fake M-Pesa, MTN MoMo, Airtel Money, Orange Money, Flutterwave and Paystack APIs.
 * Real sandbox keys take days to get, and the demo needs failures on cue, so monitors
 * point here by default. Each provider's mode is set from the Demo Control page.
 */
@RestController
@RequestMapping("/mock")
public class MockProviderController {

    /** Timeout mode waits longer than any monitor's timeout. */
    private static final long TIMEOUT_SLEEP_MS = 12_000;

    private final ProviderCatalog catalog;
    private final MockState state;

    public MockProviderController(ProviderCatalog catalog, MockState state) {
        this.catalog = catalog;
        this.state = state;
    }

    @RequestMapping("/{slug}/**")
    public ResponseEntity<Object> handle(@PathVariable String slug, HttpServletRequest request) throws InterruptedException {
        Provider provider = catalog.bySlug(slug).orElse(null);
        if (provider == null) {
            return json(HttpStatus.NOT_FOUND, Map.of("error", "Unknown mock provider: " + slug));
        }
        String path = request.getRequestURI().substring(request.getContextPath().length() + ("/mock/" + slug).length());
        Endpoint endpoint = findEndpoint(provider, request.getMethod(), path).orElse(null);
        if (endpoint == null) {
            return json(HttpStatus.NOT_FOUND, Map.of("error", "No mock for " + request.getMethod() + " " + path));
        }

        // A developer-specific problem: their own key is wrong, the provider is fine.
        if (hasInvalidKey(request)) {
            return json(HttpStatus.UNAUTHORIZED, Map.of(
                    "errorCode", "401.002.01",
                    "errorMessage", "Invalid Access Token - your credentials were rejected"));
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        MockMode mode = state.modeOf(provider.code());
        switch (mode) {
            case SLOW -> Thread.sleep(random.nextLong(2500, 4000));
            case TIMEOUT -> Thread.sleep(TIMEOUT_SLEEP_MS);
            default -> Thread.sleep(random.nextLong(60, 350));
        }

        return switch (mode) {
            case HEALTHY, SLOW, TIMEOUT -> json(HttpStatus.OK, endpoint.sampleResponse());
            case FLAKY -> random.nextBoolean()
                    ? json(HttpStatus.OK, endpoint.sampleResponse())
                    : json(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("error", "Internal Server Error", "requestId", requestId()));
            case SERVER_ERROR -> json(HttpStatus.SERVICE_UNAVAILABLE, Map.of(
                    "error", "Service Unavailable",
                    "message", provider.name() + " is temporarily unable to process requests",
                    "requestId", requestId()));
            case AUTH_ERROR -> json(HttpStatus.UNAUTHORIZED, Map.of(
                    "errorCode", "401.000.01",
                    "errorMessage", "Authentication service unavailable"));
            case RATE_LIMITED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "Too Many Requests", "message", "Rate limit of 60 requests per minute exceeded"));
            case BAD_FORMAT -> json(HttpStatus.OK, Map.of(
                    "result", "ok",
                    "payload", Map.of("info", "Response schema v2 - fields renamed"),
                    "items", Collections.emptyList()));
        };
    }

    private Optional<Endpoint> findEndpoint(Provider provider, String method, String path) {
        return provider.endpoints().stream()
                .filter(e -> e.method().equalsIgnoreCase(method))
                .filter(e -> matches(e.path(), path))
                .findFirst();
    }

    /** Compares paths ignoring the query string; {placeholders} match any single segment. */
    static boolean matches(String pattern, String path) {
        String patternPath = pattern.split("\\?")[0];
        String regex = Pattern.quote(patternPath).replaceAll("\\{[^}]+}", "\\\\E[^/]+\\\\Q");
        return path.matches(regex) || (path + "/").matches(regex) || path.matches(regex + "/");
    }

    private static boolean hasInvalidKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return auth != null && auth.toUpperCase().contains("INVALID");
    }

    private static String requestId() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    private static ResponseEntity<Object> json(HttpStatus status, Object body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
