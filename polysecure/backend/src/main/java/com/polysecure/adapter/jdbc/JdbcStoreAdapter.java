/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.jdbc;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.Condition;
import com.polysecure.model.LocalSelectQuery;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base for all JDBC-backed adapters.
 * Concrete subclasses override toSqlType(), estimateCardinality(), getCapabilities(),
 * and optionally createTable()/dropTable()/update()/delete() for dialect differences.
 */
public abstract class JdbcStoreAdapter implements StoreAdapter {

    protected final String storeName;
    protected final HikariDataSource dataSource;
    protected final JdbcTemplate jdbc;

    protected JdbcStoreAdapter(String storeName, String jdbcUrl,
                               String username, String password) {
        this.storeName = storeName;
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        if (username != null && !username.isBlank()) cfg.setUsername(username);
        if (password != null && !password.isBlank()) cfg.setPassword(password);
        cfg.setMaximumPoolSize(5);
        cfg.setPoolName("ps-" + storeName);
        cfg.setConnectionTimeout(10_000);
        this.dataSource = new HikariDataSource(cfg);
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override public String storeName() { return storeName; }

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
        String ph = values.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        jdbc.update("INSERT INTO " + table + " (" + cols + ") VALUES (" + ph + ")",
            values.values().toArray());
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        SqlBuilder b = new SqlBuilder();
        String set = updates.keySet().stream().map(k -> {
            b.addParam(updates.get(k));
            return k + " = ?";
        }).collect(Collectors.joining(", "));
        String sql = "UPDATE " + table + " SET " + set;
        if (where != null) {
            String w = b.renderCondition(where, null);
            if (!w.isBlank()) sql += " WHERE " + w;
        }
        return jdbc.update(sql, b.params().toArray());
    }

    @Override
    public int delete(String table, Condition where) {
        SqlBuilder b = new SqlBuilder();
        String sql = "DELETE FROM " + table;
        if (where != null) {
            String w = b.renderCondition(where, null);
            if (!w.isBlank()) sql += " WHERE " + w;
        }
        return jdbc.update(sql, b.params().toArray());
    }

    // ── Schema inspection ────────────────────────────────────────────────────

    @Override
    public List<ColumnDefinition> getSchema(String table) {
        String sql = """
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_name = ? ORDER BY ordinal_position
            """;
        try {
            return jdbc.query(sql, (rs, i) ->
                new ColumnDefinition(rs.getString("column_name"), rs.getString("data_type"), storeName, false),
                table);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override public void close() { dataSource.close(); }

    @Override
    public boolean ping() {
        try { jdbc.queryForObject(pingQuery(), Integer.class); return true; }
        catch (Exception e) { return false; }
    }

    protected String pingQuery() { return "SELECT 1"; }

    protected abstract String toSqlType(String type);
}
