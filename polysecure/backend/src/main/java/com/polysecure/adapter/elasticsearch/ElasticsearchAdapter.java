/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.elasticsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.*;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Adapter for Elasticsearch / OpenSearch.
 * Uses the REST API directly via Spring RestClient — no extra client dependency needed.
 * Each SQL-Poly "table" maps to an ES index.
 */
public class ElasticsearchAdapter implements StoreAdapter {

    private final String storeName;
    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public ElasticsearchAdapter(StoreConfig config) {
        this.storeName = config.name();
        String baseUrl = "http://" + config.host() + ":" + config.port();
        RestClient.Builder builder = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(buildRequestFactory());
        if (config.username() != null && !config.username().isBlank()) {
            String encoded = Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.password()).getBytes());
            builder.defaultHeader("Authorization", "Basic " + encoded);
        }
        this.http = builder.build();
    }

    private static HttpComponentsClientHttpRequestFactory buildRequestFactory() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(20);
        cm.setDefaultMaxPerRoute(10);
        var httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .evictExpiredConnections()
            .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
            .build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Override public String storeName() { return storeName; }

    // ── SELECT ──────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        String index = query.table();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", 10_000);
        if (query.where() != null) {
            body.put("query", buildQuery(query.where(), query.alias()).orElseGet(this::matchAll));
        } else {
            body.put("query", matchAll());
        }

        try {
            String response = http.post()
                .uri("/" + index + "/_search")
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            Map<String, Object> parsed = mapper.readValue(response, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> hits = (Map<String, Object>) parsed.get("hits");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");

            return hitList.stream().map(h -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> source = (Map<String, Object>) h.get("_source");
                Map<String, Object> row = new LinkedHashMap<>(source);
                row.putIfAbsent("_id", h.get("_id"));
                return applyProjections(row, query);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch select failed on index '" + index + "': " + e.getMessage(), e);
        }
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        try {
            // PUT /{index} — creates the index if it doesn't exist
            http.put().uri("/" + table)
                .header("Content-Type", "application/json")
                .body("{}")
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            // Index might already exist (400 resource_already_exists_exception) — ignore
            if (!e.getMessage().contains("resource_already_exists_exception")
                && !e.getMessage().contains("400")) throw e;
        }
    }

    @Override
    public void dropTable(String table) {
        try {
            http.delete().uri("/" + table).retrieve().toBodilessEntity();
        } catch (Exception e) {
            // index_not_found_exception — ignore
            if (!e.getMessage().contains("index_not_found")) throw e;
        }
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        try {
            http.post().uri("/" + table + "/_doc")
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(values))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch insert failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        // Use Update By Query API
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            StringBuilder script = new StringBuilder();
            updates.forEach((k, v) -> script.append("ctx._source.").append(k).append(" = params.").append(k).append("; "));
            body.put("script", Map.of("source", script.toString(), "lang", "painless", "params", updates));
            if (where != null) body.put("query", buildQuery(where, null).orElseGet(this::matchAll));
            else body.put("query", matchAll());

            String response = http.post().uri("/" + table + "/_update_by_query")
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            Map<String, Object> parsed = mapper.readValue(response, new TypeReference<>() {});
            Object updated = parsed.get("updated");
            return updated instanceof Number n ? n.intValue() : 0;
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch update failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int delete(String table, Condition where) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query", where != null ? buildQuery(where, null).orElseGet(this::matchAll) : matchAll());

            String response = http.post().uri("/" + table + "/_delete_by_query")
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            Map<String, Object> parsed = mapper.readValue(response, new TypeReference<>() {});
            Object deleted = parsed.get("deleted");
            return deleted instanceof Number n ? n.intValue() : 0;
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            String response = http.get().uri("/" + table + "/_count").retrieve().body(String.class);
            Map<String, Object> parsed = mapper.readValue(response, new TypeReference<>() {});
            Object count = parsed.get("count");
            return count instanceof Number n ? n.longValue() : 0L;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public StoreCapabilities getCapabilities() {
        return new StoreCapabilities(true, true, true, true, true, false);
    }

    @Override
    public boolean ping() {
        try { http.get().uri("/_cluster/health").retrieve().toBodilessEntity(); return true; }
        catch (Exception e) { return false; }
    }

    @Override public void close() { /* RestClient is stateless */ }

    // ── Condition → ES Query DSL ─────────────────────────────────────────────

    // Returns empty Optional when the condition belongs entirely to another store alias.
    private Optional<Map<String, Object>> buildQuery(Condition cond, String alias) {
        return switch (cond) {
            case Condition.And a -> {
                Optional<Map<String, Object>> l = buildQuery(a.left(), alias);
                Optional<Map<String, Object>> r = buildQuery(a.right(), alias);
                if (l.isEmpty() && r.isEmpty()) yield Optional.empty();
                if (l.isEmpty()) yield r;
                if (r.isEmpty()) yield l;
                yield Optional.of(Map.of("bool", Map.of("must", List.of(l.get(), r.get()))));
            }
            case Condition.Or o -> {
                Optional<Map<String, Object>> l = buildQuery(o.left(), alias);
                Optional<Map<String, Object>> r = buildQuery(o.right(), alias);
                if (l.isEmpty() || r.isEmpty()) yield Optional.empty();
                yield Optional.of(Map.of("bool", Map.of("should", List.of(l.get(), r.get()))));
            }
            case Condition.Not n -> buildQuery(n.inner(), alias)
                .map(q -> Map.of("bool", Map.of("must_not", List.of(q))));
            case Condition.Compare c -> buildCompare(c, alias);
            case Condition.In i -> {
                if (!(i.expr() instanceof Expr.Column col)) yield Optional.empty();
                if (isOtherAlias(col, alias)) yield Optional.empty();
                yield Optional.of(Map.of("terms", Map.of(col.name(), i.values())));
            }
            case Condition.IsNull isn -> {
                if (!(isn.expr() instanceof Expr.Column col)) yield Optional.empty();
                if (isOtherAlias(col, alias)) yield Optional.empty();
                yield Optional.of(Map.of("bool", Map.of("must_not",
                    List.of(Map.of("exists", Map.of("field", col.name()))))));
            }
            case Condition.Like like -> {
                if (!(like.expr() instanceof Expr.Column col)) yield Optional.empty();
                if (isOtherAlias(col, alias)) yield Optional.empty();
                String pattern = like.pattern().replace('%', '*').replace('_', '?');
                yield Optional.of(Map.of("wildcard", Map.of(col.name(), Map.of("value", pattern))));
            }
            case Condition.Between between -> {
                if (!(between.expr() instanceof Expr.Column col)) yield Optional.empty();
                if (isOtherAlias(col, alias)) yield Optional.empty();
                yield Optional.of(Map.of("range",
                    Map.of(col.name(), Map.of("gte", between.low(), "lte", between.high()))));
            }
        };
    }

    private Optional<Map<String, Object>> buildCompare(Condition.Compare c, String alias) {
        if (!(c.left() instanceof Expr.Column col) || !(c.right() instanceof Expr.Literal lit))
            return Optional.empty();
        if (isOtherAlias(col, alias)) return Optional.empty();
        String field = col.name();
        Object val = lit.value();
        Map<String, Object> q = switch (c.op()) {
            case "="        -> Map.of("term", Map.of(field, val));
            case "!=", "<>" -> Map.of("bool", Map.of("must_not", List.of(Map.of("term", Map.of(field, val)))));
            case ">"        -> Map.of("range", Map.of(field, Map.of("gt", val)));
            case "<"        -> Map.of("range", Map.of(field, Map.of("lt", val)));
            case ">="       -> Map.of("range", Map.of(field, Map.of("gte", val)));
            case "<="       -> Map.of("range", Map.of(field, Map.of("lte", val)));
            default         -> null;
        };
        return Optional.ofNullable(q);
    }

    private boolean isOtherAlias(Expr.Column col, String alias) {
        return alias != null && col.tableAlias() != null && !col.tableAlias().equals(alias);
    }

    private Map<String, Object> matchAll() { return Map.of("match_all", Map.of()); }

    private Map<String, Object> applyProjections(Map<String, Object> source, LocalSelectQuery query) {
        if (query.star() || query.projections().isEmpty()) return source;
        Map<String, Object> out = new LinkedHashMap<>();
        query.projections().forEach(p -> {
            if (source.containsKey(p.column())) out.put(p.effectiveOutputName(), source.get(p.column()));
        });
        return out;
    }
}
