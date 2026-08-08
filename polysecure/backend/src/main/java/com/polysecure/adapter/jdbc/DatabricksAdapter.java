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

public class DatabricksAdapter extends JdbcStoreAdapter {

    public DatabricksAdapter(StoreConfig config) {
        // host = workspace hostname (e.g. dbc-xxxx.cloud.databricks.com), port = 443.
        // `database` holds the SQL warehouse id. URL/property shape matches exactly what
        // the Databricks UI generates under SQL Warehouses > Connection details > JDBC:
        // "jdbc:databricks://<host>:<port>;HttpPath=/sql/1.0/warehouses/<id>" + PWD property.
        // The Databricks (Simba) driver does NOT honor the generic JDBC "user"/"password"
        // properties HikariConfig#setUsername/setPassword would set — it requires the
        // literal "UID"/"PWD" connection property names, hence extraProperties here instead
        // of the username/password constructor args.
        // EnableArrow=0 disables Arrow-based result deserialization, which relies on
        // sun.misc.Unsafe / DirectByteBuffer reflection that the JDK module system blocks
        // by default from JDK 17+ (error 500618). Falling back to the legacy row-based wire
        // format avoids the issue entirely, independent of JVM flags or JDK version.
        super(config.name(),
            "jdbc:databricks://" + config.host() + ":" + config.port()
                + ";HttpPath=/sql/1.0/warehouses/" + config.database(),
            null, null,
            Map.of("AuthMech", "3", "UID", "token", "PWD", config.password() != null ? config.password() : "",
                "EnableArrow", "0"));
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
            return jdbc.query("DESCRIBE TABLE " + table,
                (rs, i) -> new ColumnDefinition(rs.getString("col_name"), rs.getString("data_type"), storeName, false));
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public StoreCapabilities getCapabilities() { return StoreCapabilities.full(); }

    @Override
    protected String toSqlType(String type) {
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "BIGINT";
            case "TEXT", "STRING", "VARCHAR"   -> "STRING";
            case "BOOLEAN", "BOOL"             -> "BOOLEAN";
            case "FLOAT", "DOUBLE"             -> "DOUBLE";
            case "DOCUMENT"                    -> "STRING";
            default                            -> "STRING";
        };
    }
}
