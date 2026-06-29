package com.polysecure.adapter.jdbc;

import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.ColumnDefinition;

import java.util.List;

public class SnowflakeAdapter extends JdbcStoreAdapter {

    public SnowflakeAdapter(StoreConfig config) {
        // host = account.snowflakecomputing.com; port is unused (always HTTPS 443)
        super(config.name(),
            "jdbc:snowflake://" + config.host() + "/?db=" + config.database(),
            config.username(), config.password());
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Long n = jdbc.queryForObject(
                "SELECT ROW_COUNT FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = UPPER(?)",
                Long.class, table);
            if (n != null && n >= 0) return n;
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
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = UPPER(?) ORDER BY ORDINAL_POSITION",
                (rs, i) -> new ColumnDefinition(rs.getString("COLUMN_NAME"), rs.getString("DATA_TYPE"), storeName, false),
                table);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public StoreCapabilities getCapabilities() {
        return new StoreCapabilities(true, true, true, true, true, true);
    }

    @Override
    protected String toSqlType(String type) {
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "NUMBER(10) AUTOINCREMENT";
            case "TEXT", "STRING", "VARCHAR"   -> "TEXT";
            case "BOOLEAN", "BOOL"             -> "BOOLEAN";
            case "FLOAT", "DOUBLE"             -> "FLOAT";
            case "DOCUMENT"                    -> "VARIANT";
            default                            -> "TEXT";
        };
    }
}
