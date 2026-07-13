/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.catalog;

import com.polysecure.adapter.AdapterFactory;
import com.polysecure.adapter.StoreAdapter;
import com.polysecure.observability.ObservableStoreAdapter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoreRegistry {

    private final Map<String, StoreAdapter> adapters = new ConcurrentHashMap<>();
    private final Map<String, StoreConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, Gauge> upGauges = new ConcurrentHashMap<>();
    private final AdapterFactory factory;
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;

    public StoreRegistry(AdapterFactory factory, MeterRegistry meterRegistry, ObservationRegistry observationRegistry) {
        this.factory = factory;
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;
    }

    public void register(StoreConfig config) {
        StoreAdapter existing = adapters.remove(config.name());
        if (existing != null) existing.close();
        adapters.put(config.name(), new ObservableStoreAdapter(factory.create(config), observationRegistry));
        configs.put(config.name(), config);
        registerUpGauge(config);
    }

    public void unregister(String name) {
        StoreAdapter adapter = adapters.remove(name);
        if (adapter != null) adapter.close();
        configs.remove(name);
        Gauge gauge = upGauges.remove(name);
        if (gauge != null) meterRegistry.remove(gauge);
    }

    public StoreAdapter get(String name) {
        StoreAdapter adapter = adapters.get(name);
        if (adapter == null) throw new IllegalArgumentException("Store not registered: " + name);
        return adapter;
    }

    public Collection<String> listNames() {
        return adapters.keySet();
    }

    public boolean exists(String name) {
        return adapters.containsKey(name);
    }

    public List<StoreConfig> getConfigs() {
        return List.copyOf(configs.values());
    }

    private void registerUpGauge(StoreConfig config) {
        Gauge gauge = Gauge.builder("polysecure.store.up", this,
                r -> {
                    try { return r.get(config.name()).ping() ? 1 : 0; }
                    catch (Exception e) { return 0; }
                })
            .tag("store", config.name())
            .tag("type", config.type().name())
            .register(meterRegistry);
        Gauge old = upGauges.put(config.name(), gauge);
        if (old != null) meterRegistry.remove(old);
    }
}
