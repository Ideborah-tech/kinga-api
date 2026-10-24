package com.kinga.api.mock;

import com.kinga.api.model.Monitor;
import com.kinga.api.repository.MonitorRepository;
import com.kinga.api.service.MonitoringService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Controls for the live demo: break a provider for everyone, or break only your own API key. */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final String INVALID_KEY = "Bearer INVALID_KEY_demo";

    private final MockState state;
    private final MonitorRepository monitors;
    private final MonitoringService monitoring;

    public DemoController(MockState state, MonitorRepository monitors, MonitoringService monitoring) {
        this.state = state;
        this.monitors = monitors;
        this.monitoring = monitoring;
    }

    @GetMapping("/modes")
    public Map<String, MockMode> modes() {
        return state.all();
    }

    /** Sets a provider's mock mode, then re-checks all its monitors in the background. */
    @PutMapping("/modes/{provider}")
    public Map<String, MockMode> setMode(@PathVariable String provider, @RequestBody Map<String, String> body) {
        state.setMode(provider, MockMode.valueOf(body.get("mode")));
        CompletableFuture.runAsync(() -> monitoring.checkProviderNow(provider));
        return state.all();
    }

    @PostMapping("/reset")
    public Map<String, MockMode> reset(@RequestParam(required = false) String owner) {
        state.reset();
        if (owner != null) {
            for (Monitor m : monitors.findByOwnerOrderByIdAsc(owner)) {
                setInvalidKey(m, false);
            }
        }
        CompletableFuture.runAsync(() -> state.all().keySet().forEach(monitoring::checkProviderNow));
        return state.all();
    }

    /** Breaks (or fixes) the Authorization header on one developer's monitors for a provider. */
    @PostMapping("/my-key")
    public List<Long> breakMyKey(@RequestParam String owner, @RequestParam String provider,
                                 @RequestParam boolean broken) {
        List<Monitor> mine = monitors.findByOwnerOrderByIdAsc(owner).stream()
                .filter(m -> m.provider.equals(provider))
                .toList();
        mine.forEach(m -> setInvalidKey(m, broken));
        CompletableFuture.runAsync(() -> mine.forEach(m -> monitoring.checkNow(m.id)));
        return mine.stream().map(m -> m.id).toList();
    }

    /**
     * Swaps the monitor's Authorization header for an invalid one, keeping the real value in
     * X-Kinga-Original-Auth so it can be put back.
     */
    private void setInvalidKey(Monitor m, boolean broken) {
        String auth = null;
        String original = null;
        StringBuilder others = new StringBuilder();
        for (String line : m.headers == null ? new String[0] : m.headers.split("\\r?\\n")) {
            String lower = line.toLowerCase();
            if (lower.startsWith(AUTH)) auth = valueOf(line);
            else if (lower.startsWith(ORIGINAL)) original = valueOf(line);
            else if (!line.isBlank()) others.append(line).append('\n');
        }
        boolean isBroken = auth != null && auth.equals(INVALID_KEY);
        String realAuth = isBroken ? original : auth;

        if (broken) {
            others.append("Authorization: ").append(INVALID_KEY).append('\n');
            if (realAuth != null) others.append("X-Kinga-Original-Auth: ").append(realAuth).append('\n');
        } else if (realAuth != null) {
            others.append("Authorization: ").append(realAuth).append('\n');
        }
        m.headers = others.toString().trim();
        m.lastCheckedAt = null;
        monitors.save(m);
    }

    private static final String AUTH = "authorization:";
    private static final String ORIGINAL = "x-kinga-original-auth:";

    private static String valueOf(String headerLine) {
        return headerLine.substring(headerLine.indexOf(':') + 1).trim();
    }
}
