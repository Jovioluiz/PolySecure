package com.polysecure.adapter.jdbc;

import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.ColumnDefinition;

import java.util.List;
import java.util.stream.Collectors;

public class OracleAdapter extends JdbcStoreAdapter {

    public OracleAdapter(StoreConfig config) {
        super(config.name(),
            "jdbc:oracle:thin:@//" + config.host() + ":" + config.port() + "/" + config.database(),
            config.username(), config.password());
    }

    // Oracle < 23c does not support CREATE TABLE IF NOT EXISTS
    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        String cols = columns.stream()
            .map(c -> c.name() + " " + toSqlType(c.type()) + (c.primaryKey() ? " PRIMARY KEY" : ""))
            .collect(Collectors.joining(", "));
        try {
            jdbc.execute("CREATE TABLE " + table + " (" + cols + ")");
        } catch (Exception e) {
            // ORA-00955: name already used — table already exists
            if (!e.getMessage().contains("ORA-00955")) throw e;
        }
    }

    @Override
    public void dropTable(String table) {
        try {
            jdbc.execute("DROP TABLE " + table);
        } catch (Exception e) {
            // ORA-00942: table or view does not exist
            if (!e.getMessage().contains("ORA-00942")) throw e;
        }
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Long n = jdbc.queryForObject(
                "SELECT NUM_ROWS FROM all_tables WHERE OWNER = USER AND TABLE_NAME = UPPER(?)",
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
                "SELECT COLUMN_NAME, DATA_TYPE FROM all_tab_columns " +
                "WHERE OWNER = USER AND TABLE_NAME = UPPER(?) ORDER BY COLUMN_ID",
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
            case "INT", "INTEGER"              -> "NUMBER(10) GENERATED ALWAYS AS IDENTITY";
            case "TEXT", "STRING", "VARCHAR"   -> "VARCHAR2(4000)";
            case "BOOLEAN", "BOOL"             -> "NUMBER(1)";
            case "FLOAT", "DOUBLE"             -> "NUMBER(19,4)";
            case "DOCUMENT"                    -> "CLOB";
            default                            -> "VARCHAR2(4000)";
        };
    }
}
