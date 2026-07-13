/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter;

import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.Condition;
import com.polysecure.model.LocalSelectQuery;

import java.util.List;
import java.util.Map;

public interface StoreAdapter {

    String storeName();

    // Phase 1 — SELECT
    List<Map<String, Object>> select(LocalSelectQuery query);

    // Phase 2 — DDL
    void createTable(String table, List<ColumnDefinition> columns);
    void dropTable(String table);

    // Phase 2 — DML
    void insert(String table, Map<String, Object> values);
    int update(String table, Map<String, Object> updates, Condition where);
    int delete(String table, Condition where);

    // Phase 4 — Statistics
    long estimateCardinality(String table);

    // Introspection — returns native column schema for the given table
    default List<ColumnDefinition> getSchema(String table) { return List.of(); }

    // Capability declaration — what operations this store supports
    StoreCapabilities getCapabilities();

    // Phase 5 — Index management (no-op by default)
    default void createIndex(String table, String indexName, List<String> columns) {}

    // Phase 5 — Schema evolution
    default void addColumn(String table, ColumnDefinition column) {
        throw new UnsupportedOperationException("addColumn not supported by " + getClass().getSimpleName());
    }

    default void dropColumn(String table, String columnName) {
        throw new UnsupportedOperationException("dropColumn not supported by " + getClass().getSimpleName());
    }

    // Phase 6 — Observability: lightweight connectivity check for health monitoring
    default boolean ping() { return true; }

    void close();
}
