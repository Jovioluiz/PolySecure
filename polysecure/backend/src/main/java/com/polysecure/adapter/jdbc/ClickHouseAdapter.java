package com.polysecure.adapter.jdbc;

import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.Condition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickHouseAdapter extends JdbcStoreAdapter {

    public ClickHouseAdapter(StoreConfig config) {
        super(config.name(),
            "jdbc:clickhouse://" + config.host() + ":" + config.port() + "/" + config.database(),
            config.username(), config.password());
    }

    // ClickHouse requires ENGINE clause in CREATE TABLE
    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        // Skip PRIMARY KEY constraint — ClickHouse handles ordering via engine
        String cols = columns.stream()
            .map(c -> c.name() + " " + toSqlType(c.type()))
            .collect(Collectors.joining(", "));
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + table
            + " (" + cols + ") ENGINE = MergeTree() ORDER BY tuple()");
    }

    // ClickHouse uses mutations for UPDATE/DELETE (asynchronous, returns 0)
    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        SqlBuilder b = new SqlBuilder();
        String set = updates.keySet().stream().map(k -> {
            b.addParam(updates.get(k));
            return k + " = ?";
        }).collect(Collectors.joining(", "));
        String sql = "ALTER TABLE " + table + " UPDATE " + set;
        if (where != null) {
            String w = b.renderCondition(where, null);
            if (!w.isBlank()) sql += " WHERE " + w;
        }
        jdbc.update(sql, b.params().toArray());
        return 0; // mutations are eventually consistent
    }

    @Override
    public int delete(String table, Condition where) {
        SqlBuilder b = new SqlBuilder();
        String whereStr = (where != null) ? b.renderCondition(where, null) : "1";
        String sql = "ALTER TABLE " + table + " DELETE WHERE "
            + (whereStr.isBlank() ? "1" : whereStr);
        jdbc.update(sql, b.params().toArray());
        return 0;
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Long n = jdbc.queryForObject(
                "SELECT sum(rows) FROM system.parts WHERE table = ? AND active = 1",
                Long.class, table);
            if (n != null && n >= 0) return n;
        } catch (Exception ignored) {}
        try {
            Long c = jdbc.queryForObject("SELECT count() FROM " + table, Long.class);
            return c != null ? c : 0L;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public List<ColumnDefinition> getSchema(String table) {
        try {
            return jdbc.query(
                "SELECT name, type FROM system.columns WHERE table = ? AND database = currentDatabase()",
                (rs, i) -> new ColumnDefinition(rs.getString("name"), rs.getString("type"), storeName, false),
                table);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public StoreCapabilities getCapabilities() {
        // DDL supported, UPDATE/DELETE via mutations (not immediate)
        return new StoreCapabilities(true, true, true, true, true, false);
    }

    @Override
    protected String toSqlType(String type) {
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "UInt64";
            case "TEXT", "STRING", "VARCHAR"   -> "String";
            case "BOOLEAN", "BOOL"             -> "UInt8";
            case "FLOAT", "DOUBLE"             -> "Float64";
            case "DOCUMENT"                    -> "String";
            default                            -> "String";
        };
    }
}
