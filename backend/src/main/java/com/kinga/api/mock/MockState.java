package com.kinga.api.mock;

import com.kinga.api.catalog.ProviderCatalog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Current mode of every mock provider, keyed by provider code. */
@Component
public class MockState {

    private final ProviderCatalog catalog;
    private final Map<String, MockMode> modes = new ConcurrentHashMap<>();
    /** When the demo was last broken on purpose; null when everything is back to healthy. */
    private volatile Instant lastChange;

    public MockState(ProviderCatalog catalog) {
        this.catalog = catalog;
        reset();
    }

    public MockMode modeOf(String providerCode) {
        return modes.getOrDefault(providerCode, MockMode.HEALTHY);
    }

    public void setMode(String providerCode, MockMode mode) {
        modes.put(providerCode, mode);
        touch();
    }

    public void reset() {
        catalog.codes().forEach(code -> modes.put(code, MockMode.HEALTHY));
        lastChange = null;
    }

    /** Records a demo change (a provider mode or a broken key). */
    public void touch() {
        lastChange = Instant.now();
    }

    public Instant lastChange() {
        return lastChange;
    }

    public Map<String, MockMode> all() {
        Map<String, MockMode> ordered = new LinkedHashMap<>();
        catalog.codes().forEach(code -> ordered.put(code, modeOf(code)));
        return ordered;
    }
}
