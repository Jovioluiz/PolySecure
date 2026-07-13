/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.redis;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter for Redis.
 * Data model: each "row" is a Redis Hash at key "{table}:{id}".
 * SELECT returns all rows; WHERE is applied in-memory via matches().
 */
public class RedisAdapter implements StoreAdapter {

    private final String storeName;
    private final JedisPool pool;

    public RedisAdapter(StoreConfig config) {
        this.storeName = config.name();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        int db = parseDbIndex(config.database());
        if (config.password() != null && !config.password().isBlank()) {
            this.pool = new JedisPool(poolConfig, config.host(), config.port(), 2000, config.password(), db);
        } else {
            this.pool = new JedisPool(poolConfig, config.host(), config.port(), 2000, null, db);
        }
    }

    @Override public String storeName() { return storeName; }

    // ── SELECT ──────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        String prefix = query.table() + ":";
        List<Map<String, Object>> results = new ArrayList<>();
        try (Jedis jedis = pool.getResource()) {
            String cursor = "0";
            do {
                ScanResult<String> scan = jedis.scan(cursor,
                    new ScanParams().match(prefix + "*").count(1000));
                cursor = scan.getCursor();
                for (String key : scan.getResult()) {
                    Map<String, String> hash = jedis.hgetAll(key);
                    if (hash != null && !hash.isEmpty()) {
                        Map<String, Object> row = new LinkedHashMap<>(hash);
                        if (query.where() == null || matches(query.where(), row)) {
                            results.add(applyProjections(row, query));
                        }
                    }
                }
            } while (!"0".equals(cursor));
        }
        return results;
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) { /* schema-less */ }

    @Override
    public void dropTable(String table) {
        try (Jedis jedis = pool.getResource()) {
            String cursor = "0";
            do {
                ScanResult<String> scan = jedis.scan(cursor,
                    new ScanParams().match(table + ":*").count(1000));
                cursor = scan.getCursor();
                if (!scan.getResult().isEmpty()) jedis.del(scan.getResult().toArray(String[]::new));
            } while (!"0".equals(cursor));
        }
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        String id = determineId(values);
        Map<String, String> hash = new LinkedHashMap<>();
        values.forEach((k, v) -> hash.put(k, v != null ? String.valueOf(v) : ""));
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(table + ":" + id, hash);
        }
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        List<Map<String, Object>> all = select(
            new LocalSelectQuery(storeName, table, table, true, List.of(), null));
        int count = 0;
        try (Jedis jedis = pool.getResource()) {
            for (Map<String, Object> row : all) {
                if (where == null || matches(where, row)) {
                    String id = String.valueOf(row.getOrDefault("id",
                        row.values().iterator().next()));
                    Map<String, String> patch = new LinkedHashMap<>();
                    updates.forEach((k, v) -> patch.put(k, v != null ? String.valueOf(v) : ""));
                    jedis.hset(table + ":" + id, patch);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public int delete(String table, Condition where) {
        List<Map<String, Object>> all = select(
            new LocalSelectQuery(storeName, table, table, true, List.of(), null));
        int count = 0;
        try (Jedis jedis = pool.getResource()) {
            for (Map<String, Object> row : all) {
                if (where == null || matches(where, row)) {
                    String id = String.valueOf(row.getOrDefault("id",
                        row.values().iterator().next()));
                    jedis.del(table + ":" + id);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public long estimateCardinality(String table) {
        try (Jedis jedis = pool.getResource()) {
            long count = 0;
            String cursor = "0";
            do {
                ScanResult<String> scan = jedis.scan(cursor,
                    new ScanParams().match(table + ":*").count(1000));
                cursor = scan.getCursor();
                count += scan.getResult().size();
            } while (!"0".equals(cursor));
            return count;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public StoreCapabilities getCapabilities() {
        return new StoreCapabilities(true, true, true, true, false, false);
    }

    @Override
    public boolean ping() {
        try (Jedis jedis = pool.getResource()) { return "PONG".equals(jedis.ping()); }
        catch (Exception e) { return false; }
    }

    @Override public void close() { pool.close(); }

    // ── Inline condition evaluation (avoids engine→adapter circular dep) ───

    private boolean matches(Condition cond, Map<String, Object> row) {
        return switch (cond) {
            case Condition.And a     -> matches(a.left(), row) && matches(a.right(), row);
            case Condition.Or o      -> matches(o.left(), row) || matches(o.right(), row);
            case Condition.Not n     -> !matches(n.inner(), row);
            case Condition.Compare c -> compareValues(resolve(c.left(), row), c.op(), resolve(c.right(), row));
            case Condition.In i      -> {
                Object v = resolve(i.expr(), row);
                yield v != null && i.values().stream().anyMatch(val -> compareValues(v, "=", val));
            }
            case Condition.IsNull isn -> resolve(isn.expr(), row) == null;
            case Condition.Like like  -> {
                Object v = resolve(like.expr(), row);
                yield v != null && v.toString().matches(likeToRegex(like.pattern()));
            }
            case Condition.Between b -> {
                Object v = resolve(b.expr(), row);
                yield v != null && compareValues(v, ">=", b.low()) && compareValues(v, "<=", b.high());
            }
        };
    }

    private Object resolve(Expr e, Map<String, Object> row) {
        return switch (e) {
            case Expr.Literal lit -> lit.value();
            case Expr.Column col -> row.getOrDefault(col.name(), null);
            case Expr.Star s -> null;
            case Expr.Aggregate ignored -> null;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean compareValues(Object left, String op, Object right) {
        if (left == null || right == null) {
            return "=".equals(op) ? left == right : !"=".equals(op);
        }
        // Coerce strings from Redis to numbers when needed
        if (left instanceof String ls && right instanceof Number) {
            try { left = Double.parseDouble(ls); } catch (Exception ignored) {}
        }
        if (left instanceof Number l && right instanceof Number r) {
            double ld = l.doubleValue(), rd = r.doubleValue();
            return switch (op) {
                case "=" -> ld == rd; case "!=","<>" -> ld != rd;
                case ">" -> ld > rd; case "<" -> ld < rd;
                case ">=" -> ld >= rd; case "<=" -> ld <= rd; default -> false;
            };
        }
        String ls = String.valueOf(left), rs = String.valueOf(right);
        int cmp = ls.compareTo(rs);
        return switch (op) {
            case "=" -> cmp == 0; case "!=","<>" -> cmp != 0;
            case ">" -> cmp > 0; case "<" -> cmp < 0;
            case ">=" -> cmp >= 0; case "<=" -> cmp <= 0; default -> false;
        };
    }

    private String likeToRegex(String pattern) {
        StringBuilder sb = new StringBuilder("(?s)");
        for (char c : pattern.toCharArray()) {
            if (c == '%') sb.append(".*");
            else if (c == '_') sb.append(".");
            else sb.append(java.util.regex.Pattern.quote(String.valueOf(c)));
        }
        return sb.toString();
    }

    private String determineId(Map<String, Object> values) {
        Object id = values.get("id");
        return id != null ? String.valueOf(id) : UUID.randomUUID().toString();
    }

    private Map<String, Object> applyProjections(Map<String, Object> row, LocalSelectQuery query) {
        if (query.star() || query.projections().isEmpty()) return row;
        Map<String, Object> out = new LinkedHashMap<>();
        query.projections().forEach(p -> {
            if (row.containsKey(p.column())) out.put(p.effectiveOutputName(), row.get(p.column()));
        });
        return out;
    }

    private int parseDbIndex(String database) {
        try { return Integer.parseInt(database); } catch (Exception e) { return 0; }
    }
}
