package com.polysecure.engine;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * In-memory materialized view cache for cross-store SELECT queries.
 *
 * A view is created when the same query is executed at least FREQUENCY_THRESHOLD times,
 * and is automatically invalidated when any DML or DDL operation touches a store it depends on.
 * Views expire after TTL regardless of invalidation.
 */
@Component
public class MaterializedViewCache {

    static final int FREQUENCY_THRESHOLD = 2;
    static final Duration TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, CachedView> views = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> frequency = new ConcurrentHashMap<>();

    record CachedView(
        List<Map<String, Object>> rows,
        Set<String> stores,
        Instant cachedAt,
        AtomicInteger hits
    ) {}

    /**
     * Returns the cached rows for the given key if present and not expired.
     * Increments the hit counter on a cache hit.
     */
    public Optional<List<Map<String, Object>>> get(String key) {
        CachedView view = views.get(key);
        if (view == null) return Optional.empty();
        if (Instant.now().isAfter(view.cachedAt().plus(TTL))) {
            views.remove(key);
            return Optional.empty();
        }
        view.hits().incrementAndGet();
        return Optional.of(view.rows());
    }

    /**
     * Records one execution of a query key and returns true when the frequency
     * threshold is first reached (or re-reached after cache expiry).
     */
    public boolean recordExecution(String key) {
        return frequency.computeIfAbsent(key, k -> new AtomicInteger())
                        .incrementAndGet() >= FREQUENCY_THRESHOLD;
    }

    /** Stores a query result. Rows are defensively copied. */
    public void put(String key, List<Map<String, Object>> rows, Set<String> stores) {
        views.put(key, new CachedView(List.copyOf(rows), Set.copyOf(stores), Instant.now(), new AtomicInteger()));
    }

    /** Removes all views that depend on any of the given stores. */
    public void invalidateByStores(Collection<String> stores) {
        views.entrySet().removeIf(e -> !Collections.disjoint(e.getValue().stores(), stores));
    }

    /** Removes expired entries proactively (can be called by a scheduler if needed). */
    public void evictExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        views.entrySet().removeIf(e -> e.getValue().cachedAt().isBefore(cutoff));
    }

    /** Returns stats for all currently cached views, sorted by hit count descending. */
    public List<ViewStats> stats() {
        Instant now = Instant.now();
        return views.entrySet().stream()
            .map(e -> new ViewStats(
                e.getKey(),
                e.getValue().stores(),
                e.getValue().rows().size(),
                e.getValue().cachedAt(),
                Duration.between(e.getValue().cachedAt(), now),
                e.getValue().hits().get()
            ))
            .sorted(Comparator.comparingInt(ViewStats::hits).reversed())
            .collect(Collectors.toList());
    }

    public int viewCount() { return views.size(); }

    /** Clears all cached views and frequency counters. */
    public void clear() {
        views.clear();
        frequency.clear();
    }

    public record ViewStats(
        String query,
        Set<String> stores,
        int rowCount,
        Instant cachedAt,
        Duration age,
        int hits
    ) {}
}
