package com.kinga.api.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * On a shared public deployment, one visitor may leave a provider broken. After a quiet period the demo
 * goes back to healthy on its own, so the next visitor starts from green.
 */
@Component
public class DemoAutoReset {

    private static final Logger log = LoggerFactory.getLogger(DemoAutoReset.class);

    private final MockState state;
    private final DemoController demo;
    private final Duration idle;

    public DemoAutoReset(MockState state, DemoController demo,
                         @Value("${kinga.demo.auto-reset-minutes}") int minutes) {
        this.state = state;
        this.demo = demo;
        this.idle = Duration.ofMinutes(minutes);
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void resetWhenIdle() {
        Instant last = state.lastChange();
        if (idle.isZero() || last == null || last.plus(idle).isAfter(Instant.now())) return;
        log.info("Demo idle for {} min: resetting every provider and key to healthy", idle.toMinutes());
        demo.resetEverything();
    }
}
