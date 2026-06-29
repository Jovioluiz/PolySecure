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

    void close();
}
