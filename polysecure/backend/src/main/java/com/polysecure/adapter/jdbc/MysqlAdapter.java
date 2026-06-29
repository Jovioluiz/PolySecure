package com.polysecure.adapter.jdbc;

import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.ColumnDefinition;

import java.util.List;

public class MysqlAdapter extends JdbcStoreAdapter {

    public MysqlAdapter(StoreConfig config) {
        super(config.name(),
            "jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.database()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            config.username(), config.password());
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Long n = jdbc.queryForObject(
                "SELECT TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Long.class, table);
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
        try {
            return jdbc.query(
                "SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                (rs, i) -> new ColumnDefinition(rs.getString("COLUMN_NAME"), rs.getString("DATA_TYPE"), storeName, false),
                table);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public StoreCapabilities getCapabilities() { return StoreCapabilities.full(); }

    @Override
    protected String toSqlType(String type) {
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "INT AUTO_INCREMENT";
            case "TEXT", "STRING", "VARCHAR"   -> "TEXT";
            case "BOOLEAN", "BOOL"             -> "TINYINT(1)";
            case "FLOAT", "DOUBLE"             -> "DOUBLE";
            case "DOCUMENT"                    -> "JSON";
            default                            -> "TEXT";
        };
    }
}
