/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.neo4j;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.*;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.*;
import java.util.stream.Collectors;

public class Neo4jAdapter implements StoreAdapter {

    private final String storeName;
    private final Driver driver;

    public Neo4jAdapter(StoreConfig config) {
        this.storeName = config.name();
        AuthToken auth = (config.username() != null && !config.username().isBlank())
            ? AuthTokens.basic(config.username(), config.password())
            : AuthTokens.none();
        this.driver = GraphDatabase.driver(
            "bolt://" + config.host() + ":" + config.port(), auth);
    }

    @Override
    public String storeName() { return storeName; }

    // ── SELECT ──────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        String label = query.table();
        String cypher = buildMatch(label, query);
        try (Session session = driver.session()) {
            return session.run(cypher).list(this::recordToMap);
        }
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        // Create a uniqueness constraint on the primary key if one is defined
        columns.stream()
            .filter(ColumnDefinition::primaryKey)
            .findFirst()
            .ifPresent(pk -> {
                try (Session s = driver.session()) {
                    s.run("CREATE CONSTRAINT IF NOT EXISTS FOR (n:" + table
                        + ") REQUIRE n." + pk.name() + " IS UNIQUE");
                }
            });
    }

    @Override
    public void dropTable(String table) {
        try (Session session = driver.session()) {
            session.run("MATCH (n:" + table + ") DETACH DELETE n");
        }
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        try (Session session = driver.session()) {
            session.run("CREATE (n:" + table + " $props)", Map.of("props", values));
        }
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        CypherBuilder builder = new CypherBuilder();
        String whereClause = where != null ? builder.renderCondition(where) : "";
        String setClause = updates.entrySet().stream()
            .map(e -> "n." + e.getKey() + " = $" + e.getKey())
            .collect(Collectors.joining(", "));
        Map<String, Object> params = new HashMap<>(updates);
        params.putAll(builder.params());

        String cypher = "MATCH (n:" + table + ")"
            + (whereClause.isBlank() ? "" : " WHERE " + whereClause)
            + " SET " + setClause
            + " RETURN count(n) AS affected";

        try (Session session = driver.session()) {
            var result = session.run(cypher, params);
            return result.single().get("affected").asInt();
        }
    }

    @Override
    public int delete(String table, Condition where) {
        CypherBuilder builder = new CypherBuilder();
        String whereClause = where != null ? builder.renderCondition(where) : "";
        String cypher = "MATCH (n:" + table + ")"
            + (whereClause.isBlank() ? "" : " WHERE " + whereClause)
            + " WITH n, count(n) AS cnt DETACH DELETE n RETURN cnt";

        try (Session session = driver.session()) {
            var result = session.run(cypher, builder.params());
            return result.hasNext() ? result.next().get("cnt").asInt() : 0;
        }
    }

    @Override
    public long estimateCardinality(String table) {
        try (Session session = driver.session()) {
            var result = session.run("MATCH (n:" + table + ") RETURN count(n) AS cnt");
            return result.single().get("cnt").asLong();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public StoreCapabilities getCapabilities() {
        // Neo4j supports graph traversal; UPDATE is done via SET but partial; no relational transactions
        return new StoreCapabilities(true, true, true, true, true, false);
    }

    @Override
    public boolean ping() {
        try { driver.verifyConnectivity(); return true; }
        catch (Exception e) { return false; }
    }

    @Override
    public void close() { driver.close(); }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String buildMatch(String label, LocalSelectQuery query) {
        CypherBuilder builder = new CypherBuilder();
        String where = query.where() != null ? builder.renderCondition(query.where()) : "";
        String ret = buildReturn(query);
        return "MATCH (n:" + label + ")"
            + (where.isBlank() ? "" : " WHERE " + where)
            + " RETURN " + ret;
    }

    private String buildReturn(LocalSelectQuery query) {
        if (query.star() || query.projections().isEmpty()) return "properties(n)";
        return query.projections().stream()
            .map(c -> "n." + c.column() + " AS " + c.effectiveOutputName())
            .collect(Collectors.joining(", "));
    }

    private Map<String, Object> recordToMap(Record record) {
        Map<String, Object> map = new LinkedHashMap<>();
        // If the query returned properties(n), it comes as a map
        if (record.fields().size() == 1 && record.fields().get(0).key().equals("properties(n)")) {
            Value v = record.get("properties(n)");
            v.asMap().forEach(map::put);
        } else {
            record.fields().forEach(f -> map.put(f.key(), f.value().asObject()));
        }
        return map;
    }

    // Translates Condition tree to Cypher WHERE clause using named parameters ($param0...)
    static final class CypherBuilder {
        private final Map<String, Object> params = new LinkedHashMap<>();
        private int counter = 0;

        Map<String, Object> params() { return params; }

        String renderCondition(Condition cond) {
            return switch (cond) {
                case Condition.And a -> "(" + renderCondition(a.left()) + " AND " + renderCondition(a.right()) + ")";
                case Condition.Or  o -> "(" + renderCondition(o.left()) + " OR "  + renderCondition(o.right()) + ")";
                case Condition.Not n -> "NOT (" + renderCondition(n.inner()) + ")";
                case Condition.Compare c -> renderCompare(c);
                case Condition.In i -> {
                    if (!(i.expr() instanceof Expr.Column col)) yield "true";
                    String name = "p" + counter++;
                    params.put(name, i.values());
                    yield "n." + col.name() + " IN $" + name;
                }
                case Condition.IsNull isn -> {
                    if (!(isn.expr() instanceof Expr.Column col)) yield "true";
                    yield "n." + col.name() + " IS NULL";
                }
                case Condition.Like like -> {
                    if (!(like.expr() instanceof Expr.Column col)) yield "true";
                    String name = "p" + counter++;
                    params.put(name, likeToRegex(like.pattern()));
                    yield "n." + col.name() + " =~ $" + name;
                }
                case Condition.Between between -> {
                    if (!(between.expr() instanceof Expr.Column col)) yield "true";
                    String lo = "p" + counter++;
                    String hi = "p" + counter++;
                    params.put(lo, between.low());
                    params.put(hi, between.high());
                    yield "(n." + col.name() + " >= $" + lo + " AND n." + col.name() + " <= $" + hi + ")";
                }
            };
        }

        private String likeToRegex(String pattern) {
            StringBuilder regex = new StringBuilder("(?s)");
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                if (c == '%') {
                    regex.append(".*");
                } else if (c == '_') {
                    regex.append(".");
                } else {
                    regex.append(java.util.regex.Pattern.quote(String.valueOf(c)));
                }
            }
            return regex.toString();
        }

        private String renderCompare(Condition.Compare c) {
            String left = renderExpr(c.left());
            String right = renderExpr(c.right());
            String op = "!=".equals(c.op()) || "<>".equals(c.op()) ? "<>" : c.op();
            return left + " " + op + " " + right;
        }

        private String renderExpr(Expr e) {
            return switch (e) {
                case Expr.Column col -> "n." + col.name();
                case Expr.Literal lit -> {
                    String name = "p" + counter++;
                    params.put(name, lit.value());
                    yield "$" + name;
                }
                case Expr.Star s -> "*";
                case Expr.Aggregate ignored -> "null";
            };
        }
    }
}
