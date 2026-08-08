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
import java.util.stream.Collectors;

public class SqlServerAdapter extends JdbcStoreAdapter {

    public SqlServerAdapter(StoreConfig config) {
        super(config.name(),
            "jdbc:sqlserver://" + config.host() + ":" + config.port()
                + ";databaseName=" + config.database() + ";encrypt=false",
            config.username(), config.password());
    }

    // SQL Server does not support CREATE TABLE IF NOT EXISTS before 2016
    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        String cols = columns.stream()
            .map(c -> c.name() + " " + toSqlType(c.type()) + (c.primaryKey() ? " PRIMARY KEY" : ""))
            .collect(Collectors.joining(", "));
        jdbc.execute("IF OBJECT_ID(N'" + table + "', N'U') IS NULL "
            + "CREATE TABLE " + table + " (" + cols + ")");
    }

    @Override
    public void dropTable(String table) {
        jdbc.execute("IF OBJECT_ID(N'" + table + "', N'U') IS NOT NULL DROP TABLE " + table);
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Long n = jdbc.queryForObject(
                "SELECT SUM(p.rows) FROM sys.partitions p " +
                "JOIN sys.objects o ON p.object_id = o.object_id " +
                "WHERE o.name = ? AND p.index_id IN (0,1)",
                Long.class, table);
            return n != null ? n : 0L;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public List<ColumnDefinition> getSchema(String table) {
        try {
            return jdbc.query(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
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
            case "INT", "INTEGER"              -> "INT";
            case "TEXT", "STRING", "VARCHAR"   -> "NVARCHAR(MAX)";
            case "BOOLEAN", "BOOL"             -> "BIT";
            case "FLOAT", "DOUBLE"             -> "FLOAT";
            case "DOCUMENT"                    -> "NVARCHAR(MAX)";
            default                            -> "NVARCHAR(MAX)";
        };
    }
}
