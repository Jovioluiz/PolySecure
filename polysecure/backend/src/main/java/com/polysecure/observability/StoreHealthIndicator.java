/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.observability;

import com.polysecure.catalog.StoreRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Aggregates a ping() per registered store under /actuator/health -> components.stores.
 * Overall status is DOWN if any store fails to respond.
 */
@Component("stores")
public class StoreHealthIndicator implements HealthIndicator {

    private final StoreRegistry registry;

    public StoreHealthIndicator(StoreRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        boolean allUp = true;
        for (String name : registry.listNames()) {
            boolean up;
            try { up = registry.get(name).ping(); }
            catch (Exception e) { up = false; }
            builder.withDetail(name, up ? "UP" : "DOWN");
            if (!up) allUp = false;
        }
        return (allUp ? builder.up() : builder.down()).build();
    }
}
