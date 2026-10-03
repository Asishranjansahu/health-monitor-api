package com.healthmonitor.api.service;

import com.healthmonitor.api.model.AlertType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Alert cooldown: each (patient, alert type) pair fires at most once per
 * configured duration (default one minute). State is per JVM instance and
 * resets on restart, which is documented in the README.
 */
@Component
public class AlertCooldown {

    private final Duration ttl;
    private final Clock clock;
    private final ConcurrentMap<String, Instant> lastFired = new ConcurrentHashMap<>();

    @Autowired
    public AlertCooldown(@Value("${healthmonitor.alert.cooldown-seconds}") long cooldownSeconds) {
        this(Duration.ofSeconds(cooldownSeconds), Clock.systemUTC());
    }

    AlertCooldown(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    /**
     * Record an attempt to fire an alert.
     *
     * @return true if the alert may fire now, false if it is still cooling down.
     */
    public boolean tryFire(Long patientId, AlertType type) {
        String key = patientId + ":" + type;
        Instant now = clock.instant();
        boolean[] allowed = { false };
        lastFired.compute(key, (k, previous) -> {
            if (previous == null || !now.isBefore(previous.plus(ttl))) {
                allowed[0] = true;
                return now;
            }
            return previous;
        });
        return allowed[0];
    }
}
