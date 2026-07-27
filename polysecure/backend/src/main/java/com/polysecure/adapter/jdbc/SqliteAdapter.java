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

public class SqliteAdapter extends JdbcStoreAdapter {

    public SqliteAdapter(StoreConfig config) {
        // SQLite is an embedded file database — host/port/username/password are unused;
        // `database` holds the file path (or ":memory:" for an ephemeral in-memory DB).
        super(config.name(), "jdbc:sqlite:" + config.database(), null, null);
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            return c != null ? c : 0L;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public List<ColumnDefinition> getSchema(String table) {
        try {
            return jdbc.query("PRAGMA table_info(" + table + ")",
                (rs, i) -> new ColumnDefinition(rs.getString("name"), rs.getString("type"),
                    storeName, rs.getInt("pk") > 0));
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public StoreCapabilities getCapabilities() { return StoreCapabilities.full(); }

    @Override
    protected String toSqlType(String type) {
        // Plain "INTEGER" (not "INTEGER PRIMARY KEY AUTOINCREMENT") — the base
        // createTable() already appends " PRIMARY KEY" when the column is a PK,
        // and SQLite's rowid-aliasing already auto-increments a plain INTEGER PK.
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "INTEGER";
            case "TEXT", "STRING", "VARCHAR"   -> "TEXT";
            case "BOOLEAN", "BOOL"             -> "INTEGER";
            case "FLOAT", "DOUBLE"             -> "REAL";
            case "DOCUMENT"                    -> "TEXT";
            default                            -> "TEXT";
        };
    }
}
