/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.ColumnRef;
import com.polysecure.model.Condition;
import com.polysecure.model.Expr;
import com.polysecure.model.LocalSelectQuery;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter for Apache Cassandra using the DataStax Java Driver 4.x.
 * Each SQL-Poly "table" maps to a Cassandra table in the configured keyspace.
 * AND / Compare / In / Between conditions are pushed down as CQL WHERE with ALLOW FILTERING.
 * OR, NOT, IsNull, Like are not pushed down — QueryEngine filters those in memory.
 */
public class CassandraAdapter implements StoreAdapter {

    private final String storeName;
    private final CqlSession session;
    private final String keyspace;

    public CassandraAdapter(StoreConfig config) {
        this.storeName = config.name();
        this.keyspace  = config.database();
        var builder = CqlSession.builder()
            .addContactPoint(new InetSocketAddress(config.host(), config.port()))
            .withLocalDatacenter("datacenter1")
            .withKeyspace(keyspace);
        if (config.username() != null && !config.username().isBlank()) {
            builder = builder.withAuthCredentials(config.username(), config.password());
        }
        this.session = builder.build();
    }

    @Override public String storeName() { return storeName; }

    // ── SELECT ──────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        CqlBuilder builder = new CqlBuilder();
        String cols = buildSelectCols(query);
        String where = query.where() != null ? builder.buildWhere(query.where()) : "";
        String cql = "SELECT " + cols + " FROM " + query.table()
            + (where.isBlank() ? "" : " WHERE " + where)
            + " ALLOW FILTERING";

        SimpleStatement stmt = SimpleStatement.newInstance(cql, builder.params().toArray());
        return session.execute(stmt).all().stream()
            .map(this::rowToMap)
            .collect(Collectors.toList());
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        String pk = columns.stream().filter(ColumnDefinition::primaryKey)
            .map(ColumnDefinition::name).findFirst().orElse(columns.get(0).name());
        String cols = columns.stream()
            .map(c -> c.name() + " " + toCqlType(c.type()))
            .collect(Collectors.joining(", "));
        session.execute("CREATE TABLE IF NOT EXISTS " + table
            + " (" + cols + ", PRIMARY KEY (" + pk + "))");
    }

    @Override
    public void dropTable(String table) {
        session.execute("DROP TABLE IF EXISTS " + table);
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        String cols = String.join(", ", values.keySet());
        String ph = values.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        SimpleStatement stmt = SimpleStatement.newInstance(
            "INSERT INTO " + table + " (" + cols + ") VALUES (" + ph + ")",
            values.values().toArray());
        session.execute(stmt);
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        CqlBuilder builder = new CqlBuilder();
        String set = updates.entrySet().stream().map(e -> {
            builder.addParam(e.getValue());
            return e.getKey() + " = ?";
        }).collect(Collectors.joining(", "));
        String whereStr = where != null ? builder.buildWhere(where) : "";
        String cql = "UPDATE " + table + " SET " + set
            + (whereStr.isBlank() ? "" : " WHERE " + whereStr);
        session.execute(SimpleStatement.newInstance(cql, builder.params().toArray()));
        return 0;
    }

    @Override
    public int delete(String table, Condition where) {
        CqlBuilder builder = new CqlBuilder();
        String whereStr = where != null ? builder.buildWhere(where) : "";
        String cql = "DELETE FROM " + table
            + (whereStr.isBlank() ? "" : " WHERE " + whereStr);
        session.execute(SimpleStatement.newInstance(cql, builder.params().toArray()));
        return 0;
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            Row row = session.execute("SELECT count(*) FROM " + table).one();
            return row != null ? row.getLong(0) : 0L;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public List<ColumnDefinition> getSchema(String table) {
        try {
            String cql = "SELECT column_name, type FROM system_schema.columns "
                + "WHERE keyspace_name = ? AND table_name = ?";
            return session.execute(SimpleStatement.newInstance(cql, keyspace, table))
                .all().stream()
                .map(r -> new ColumnDefinition(
                    r.getString("column_name"), r.getString("type"), storeName, false))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public StoreCapabilities getCapabilities() {
        return new StoreCapabilities(true, true, true, true, true, false);
    }

    @Override
    public boolean ping() {
        try { session.execute("SELECT release_version FROM system.local"); return true; }
        catch (Exception e) { return false; }
    }

    @Override public void close() { session.close(); }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String buildSelectCols(LocalSelectQuery query) {
        if (query.star() || query.projections().isEmpty()) return "*";
        return query.projections().stream().map(ColumnRef::column).collect(Collectors.joining(", "));
    }

    private Map<String, Object> rowToMap(Row row) {
        Map<String, Object> map = new LinkedHashMap<>();
        // col here is inferred as com.datastax.oss.driver.api.core.cql.ColumnDefinition (no ambiguity)
        row.getColumnDefinitions().forEach(col ->
            map.put(col.getName().asInternal(), row.getObject(col.getName())));
        return map;
    }

    private String toCqlType(String type) {
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "int";
            case "TEXT", "STRING", "VARCHAR"   -> "text";
            case "BOOLEAN", "BOOL"             -> "boolean";
            case "FLOAT", "DOUBLE"             -> "double";
            case "DOCUMENT"                    -> "text";
            default                            -> "text";
        };
    }

    // ── CQL condition builder ────────────────────────────────────────────────

    static final class CqlBuilder {
        private final List<Object> params = new ArrayList<>();
        void addParam(Object v) { params.add(v); }
        List<Object> params() { return params; }

        String buildWhere(Condition cond) {
            return switch (cond) {
                case Condition.And a -> {
                    String l = buildWhere(a.left()), r = buildWhere(a.right());
                    yield l.isBlank() || r.isBlank() ? l + r : "(" + l + " AND " + r + ")";
                }
                case Condition.Compare c -> buildCompare(c);
                case Condition.In i -> {
                    if (!(i.expr() instanceof Expr.Column col)) yield "";
                    i.values().forEach(params::add);
                    String ph = i.values().stream().map(v -> "?").collect(Collectors.joining(", "));
                    yield col.name() + " IN (" + ph + ")";
                }
                case Condition.Between b -> {
                    if (!(b.expr() instanceof Expr.Column col)) yield "";
                    params.add(b.low());
                    params.add(b.high());
                    yield col.name() + " >= ? AND " + col.name() + " <= ?";
                }
                // OR, NOT, IsNull, Like — not pushed down; QueryEngine filters in memory
                default -> "";
            };
        }

        private String buildCompare(Condition.Compare c) {
            if (!(c.left() instanceof Expr.Column col)) return "";
            if (!(c.right() instanceof Expr.Literal lit)) return "";
            String op = ("!=".equals(c.op()) || "<>".equals(c.op())) ? "!=" : c.op();
            params.add(lit.value());
            return col.name() + " " + op + " ?";
        }
    }
}
