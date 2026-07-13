/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.postgres;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.adapter.jdbc.SqlBuilder;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class PostgresAdapter implements StoreAdapter {

    private final String storeName;
    private final HikariDataSource dataSource;
    private final JdbcTemplate jdbc;

    public PostgresAdapter(StoreConfig config) {
        this.storeName = config.name();
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:postgresql://" + config.host() + ":" + config.port() + "/" + config.database());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(5);
        hikari.setPoolName("ps-" + config.name());
        this.dataSource = new HikariDataSource(hikari);
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public String storeName() { return storeName; }

    // ── SELECT ──────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        SqlBuilder builder = new SqlBuilder();
        String sql = builder.build(query);
        return jdbc.queryForList(sql, builder.params().toArray());
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        String cols = columns.stream()
            .map(c -> c.name() + " " + toSqlType(c.type()) + (c.primaryKey() ? " PRIMARY KEY" : ""))
            .collect(Collectors.joining(", "));
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table + " (" + cols + ")");
    }

    @Override
    public void dropTable(String table) {
        jdbc.execute("DROP TABLE IF EXISTS " + table);
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        String cols = String.join(", ", values.keySet());
        String placeholders = values.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        jdbc.update("INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")",
            values.values().toArray());
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        SqlBuilder builder = new SqlBuilder();
        String setClause = updates.keySet().stream().map(k -> {
            builder.addParam(updates.get(k));
            return k + " = ?";
        }).collect(Collectors.joining(", "));
        String sql = "UPDATE " + table + " SET " + setClause;
        if (where != null) {
            String whereStr = builder.renderCondition(where, null);
            if (!whereStr.isBlank()) sql += " WHERE " + whereStr;
        }
        return jdbc.update(sql, builder.params().toArray());
    }

    @Override
    public int delete(String table, Condition where) {
        SqlBuilder builder = new SqlBuilder();
        String sql = "DELETE FROM " + table;
        if (where != null) {
            String whereStr = builder.renderCondition(where, null);
            if (!whereStr.isBlank()) sql += " WHERE " + whereStr;
        }
        return jdbc.update(sql, builder.params().toArray());
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Long n = jdbc.queryForObject(
                "SELECT reltuples::bigint FROM pg_class WHERE relname = ?", Long.class, table);
            if (n != null && n > 0) return n;
        } catch (Exception ignored) {}
        try {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            return c != null ? c : 0L;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public List<ColumnDefinition> getSchema(String table) {
        String sql = """
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_name = ? AND table_schema = 'public'
            ORDER BY ordinal_position
            """;
        try {
            return jdbc.query(sql, (rs, i) -> new ColumnDefinition(
                rs.getString("column_name"), rs.getString("data_type"), storeName, false), table);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void createIndex(String table, String indexName, List<String> columns) {
        String cols = String.join(", ", columns);
        jdbc.execute("CREATE INDEX IF NOT EXISTS " + indexName + " ON " + table + " (" + cols + ")");
    }

    @Override
    public void addColumn(String table, ColumnDefinition column) {
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS "
            + column.name() + " " + toSqlType(column.type()));
    }

    @Override
    public void dropColumn(String table, String columnName) {
        jdbc.execute("ALTER TABLE " + table + " DROP COLUMN IF EXISTS " + columnName);
    }

    @Override
    public StoreCapabilities getCapabilities() { return StoreCapabilities.full(); }

    @Override
    public boolean ping() {
        try { jdbc.queryForObject("SELECT 1", Integer.class); return true; }
        catch (Exception e) { return false; }
    }

    @Override
    public void close() { dataSource.close(); }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String toSqlType(String type) {
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "INTEGER";
            case "TEXT", "STRING", "VARCHAR"   -> "TEXT";
            case "BOOLEAN", "BOOL"             -> "BOOLEAN";
            case "FLOAT", "DOUBLE"             -> "DOUBLE PRECISION";
            case "DOCUMENT"                    -> "JSONB";
            default                            -> "TEXT";
        };
    }
}
