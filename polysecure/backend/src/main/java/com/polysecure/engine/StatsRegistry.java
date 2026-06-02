package com.polysecure.engine;

import com.polysecure.catalog.StoreRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cardinality cache with 60-second TTL per (store, table) pair.
 * Falls back to Long.MAX_VALUE on error so unknown tables are treated as expensive.
 */
@Component
public class StatsRegistry {

    private static final long TTL_MS = 60_000;

    private final StoreRegistry registry;
    private final ConcurrentHashMap<String, CachedStat> cache = new ConcurrentHashMap<>();

    public StatsRegistry(StoreRegistry registry) {
        this.registry = registry;
    }

    public long cardinality(String storeName, String table) {
        String key = storeName + "." + table;
        CachedStat cached = cache.get(key);
        if (cached != null && !cached.isExpired()) return cached.value();
        long value;
        try {
            value = registry.get(storeName).estimateCardinality(table);
        } catch (Exception e) {
            value = Long.MAX_VALUE;
        }
        cache.put(key, new CachedStat(value, System.currentTimeMillis() + TTL_MS));
        return value;
    }

    public void invalidate(String storeName, String table) {
        cache.remove(storeName + "." + table);
    }

    public Map<String, Long> snapshot() {
        Map<String, Long> result = new LinkedHashMap<>();
        cache.forEach((k, v) -> { if (!v.isExpired()) result.put(k, v.value()); });
        return result;
    }

    private record CachedStat(long value, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
}
