/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.jdbc;

import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.ColumnDefinition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DolphinDbAdapter extends JdbcStoreAdapter {

    // Pool pinned to 1 connection: createTable()'s plain (unshared) DolphinDB table is only
    // visible on the session that created it, so every statement must reuse that same session.
    public DolphinDbAdapter(StoreConfig config) {
        super(config.name(),
            "jdbc:dolphindb://" + config.host() + ":" + config.port(),
            config.username(), config.password(), Map.of(), 1);
    }

    // DolphinDB's SQL dialect has no CREATE TABLE IF NOT EXISTS — tolerate "already exists"
    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        String cols = columns.stream()
            .map(c -> c.name() + " " + toSqlType(c.type()))
            .collect(Collectors.joining(", "));
        try {
            jdbc.execute("CREATE TABLE " + table + " (" + cols + ")");
        } catch (Exception e) {
            if (!e.getMessage().toLowerCase().contains("already exists")) throw e;
        }
    }

    @Override
    public void dropTable(String table) {
        try {
            jdbc.execute("DROP TABLE " + table);
        } catch (Exception e) {
            // DolphinDB phrases this as "Table doesn't exist." — doesn't contain "not exist"
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (!msg.contains("not exist") && !msg.contains("doesn't exist")) throw e;
        }
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Long c = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
            return c != null ? c : 0L;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    // No ANSI information_schema equivalent — schema inspection is not supported
    @Override
    public List<ColumnDefinition> getSchema(String table) { return List.of(); }

    @Override
    public StoreCapabilities getCapabilities() {
        // DDL/DML supported; distributed/partitioned tables have no classic multi-statement
        // transaction guarantees, same caveat as other columnar analytical stores.
        return new StoreCapabilities(true, true, true, true, true, false);
    }

    @Override
    protected String toSqlType(String type) {
        // DolphinDB has no auto-increment column — primary-key uniqueness is caller's responsibility
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "LONG";
            case "TEXT", "STRING", "VARCHAR"   -> "STRING";
            case "BOOLEAN", "BOOL"             -> "BOOL";
            case "FLOAT", "DOUBLE"             -> "DOUBLE";
            case "DOCUMENT"                    -> "STRING";
            default                            -> "STRING";
        };
    }
}
