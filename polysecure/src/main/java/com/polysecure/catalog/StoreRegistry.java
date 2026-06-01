package com.polysecure.catalog;

import com.polysecure.adapter.AdapterFactory;
import com.polysecure.adapter.StoreAdapter;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoreRegistry {

    private final Map<String, StoreAdapter> adapters = new ConcurrentHashMap<>();
    private final AdapterFactory factory;

    public StoreRegistry(AdapterFactory factory) {
        this.factory = factory;
    }

    public void register(StoreConfig config) {
        StoreAdapter existing = adapters.remove(config.name());
        if (existing != null) existing.close();
        adapters.put(config.name(), factory.create(config));
    }

    public void unregister(String name) {
        StoreAdapter adapter = adapters.remove(name);
        if (adapter != null) adapter.close();
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
}
