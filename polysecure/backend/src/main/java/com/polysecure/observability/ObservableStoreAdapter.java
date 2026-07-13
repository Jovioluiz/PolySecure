/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.observability;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.Condition;
import com.polysecure.model.LocalSelectQuery;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Decorates a StoreAdapter so every store round-trip is timed and traced.
 * Wrapping happens once in StoreRegistry.register(), so QueryEngine, DmlExecutor
 * and DdlExecutor all get per-store metrics/spans for free via registry.get(name).
 */
public class ObservableStoreAdapter implements StoreAdapter {

    private final StoreAdapter delegate;
    private final ObservationRegistry observationRegistry;

    public ObservableStoreAdapter(StoreAdapter delegate, ObservationRegistry observationRegistry) {
        this.delegate = delegate;
        this.observationRegistry = observationRegistry;
    }

    public StoreAdapter delegate() { return delegate; }

    @Override public String storeName() { return delegate.storeName(); }

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        return observe("select", () -> delegate.select(query));
    }

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        observeVoid("createTable", () -> delegate.createTable(table, columns));
    }

    @Override
    public void dropTable(String table) {
        observeVoid("dropTable", () -> delegate.dropTable(table));
    }

    @Override
    public void insert(String table, Map<String, Object> values) {
        observeVoid("insert", () -> delegate.insert(table, values));
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        return observe("update", () -> delegate.update(table, updates, where));
    }

    @Override
    public int delete(String table, Condition where) {
        return observe("delete", () -> delegate.delete(table, where));
    }

    @Override
    public long estimateCardinality(String table) {
        return observe("estimateCardinality", () -> delegate.estimateCardinality(table));
    }

    @Override
    public List<ColumnDefinition> getSchema(String table) {
        return delegate.getSchema(table);
    }

    @Override
    public StoreCapabilities getCapabilities() {
        return delegate.getCapabilities();
    }

    @Override
    public void createIndex(String table, String indexName, List<String> columns) {
        observeVoid("createIndex", () -> delegate.createIndex(table, indexName, columns));
    }

    @Override
    public void addColumn(String table, ColumnDefinition column) {
        observeVoid("addColumn", () -> delegate.addColumn(table, column));
    }

    @Override
    public void dropColumn(String table, String columnName) {
        observeVoid("dropColumn", () -> delegate.dropColumn(table, columnName));
    }

    // ping() is used by health checks; call it directly so frequent health polling
    // doesn't pollute query latency metrics and traces.
    @Override
    public boolean ping() { return delegate.ping(); }

    @Override
    public void close() { delegate.close(); }

    private <T> T observe(String operation, Supplier<T> action) {
        return Observation.createNotStarted("polysecure.store", observationRegistry)
            .lowCardinalityKeyValue("store", delegate.storeName())
            .lowCardinalityKeyValue("operation", operation)
            .observe(action::get);
    }

    private void observeVoid(String operation, Runnable action) {
        observe(operation, () -> { action.run(); return null; });
    }
}
