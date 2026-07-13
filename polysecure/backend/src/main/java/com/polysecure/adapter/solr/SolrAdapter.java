/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.solr;

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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Adapter for Apache Solr.
 * Uses the Solr JSON API via Spring RestClient.
 * Each SQL-Poly "table" maps to a Solr collection.
 */
public class SolrAdapter implements StoreAdapter {

    private final String storeName;
    private final String baseUrl;
    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int maxRows;

    public SolrAdapter(StoreConfig config, int maxRows) {
        this.storeName = config.name();
        this.maxRows = maxRows;
        this.baseUrl = "http://" + config.host() + ":" + config.port();
        this.http = RestClient.builder()
            .baseUrl(this.baseUrl)
            .requestFactory(buildRequestFactory())
            .build();
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
        String collection = query.table();
        String q = query.where() != null ? buildSolrQuery(query.where(), query.alias()) : "*:*";
        if (q.isBlank()) q = "*:*";

        String fl = buildFieldList(query);

        try {
            // Solr range queries contain literal '{' and '}' which Spring's UriBuilder
            // misinterprets as URI template variables — build the URI manually instead.
            String uriPath = "/solr/" + collection + "/select?q="
                + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&rows=" + maxRows + "&wt=json";
            if (!fl.isBlank())
                uriPath += "&fl=" + URLEncoder.encode(fl, StandardCharsets.UTF_8);

            String response = http.get()
                .uri(URI.create(baseUrl + uriPath))
                .retrieve()
                .body(String.class);
            Map<String, Object> parsed = mapper.readValue(response, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> responseSection = (Map<String, Object>) parsed.get("response");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> docs = (List<Map<String, Object>>) responseSection.get("docs");
            return docs != null ? docs : List.of();
        } catch (Exception e) {
            throw new RuntimeException("Solr select failed on collection '" + collection + "': " + e.getMessage(), e);
        }
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        // Solr collections are typically pre-created via Solr Admin; attempt API creation
        try {
            http.get()
                .uri("/solr/admin/collections?action=CREATE&name=" + table + "&numShards=1&replicationFactor=1")
                .retrieve()
                .toBodilessEntity();
        } catch (Exception ignored) {
            // Collection may already exist or API not available in embedded mode
        }
    }

    @Override
    public void dropTable(String table) {
        try {
            http.get()
                .uri("/solr/admin/collections?action=DELETE&name=" + table)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception ignored) {}
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        try {
            String json = "[" + mapper.writeValueAsString(values) + "]";
            http.post()
                .uri("/solr/" + table + "/update/json?commit=true")
                .header("Content-Type", "application/json")
                .body(json)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Solr insert failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        // Solr uses atomic updates or full document replacement; use add with overwrite
        String q = where != null ? buildSolrQuery(where, null) : "*:*";
        if (q.isBlank()) q = "*:*";
        // For simplicity: fetch matching docs, merge updates, re-index
        List<Map<String, Object>> docs = select(new LocalSelectQuery(storeName, table, table, true, List.of(), where));
        docs.forEach(doc -> {
            Map<String, Object> merged = new LinkedHashMap<>(doc);
            merged.putAll(updates);
            try { insert(table, merged); } catch (Exception ignored) {}
        });
        return docs.size();
    }

    @Override
    public int delete(String table, Condition where) {
        String q = where != null ? buildSolrQuery(where, null) : "*:*";
        if (q.isBlank()) q = "*:*";
        try {
            String body = "{\"delete\":{\"query\":\"" + q.replace("\"", "\\\"") + "\"}}";
            http.post()
                .uri("/solr/" + table + "/update?commit=true")
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .toBodilessEntity();
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Solr delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            String response = http.get()
                .uri(b -> b.path("/solr/" + table + "/select")
                           .queryParam("q", "*:*")
                           .queryParam("rows", "0")
                           .queryParam("wt", "json")
                           .build())
                .retrieve()
                .body(String.class);
            Map<String, Object> parsed = mapper.readValue(response, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> responseSection = (Map<String, Object>) parsed.get("response");
            Object numFound = responseSection.get("numFound");
            return numFound instanceof Number n ? n.longValue() : 0L;
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
        try {
            http.get().uri("/solr/admin/info/system?wt=json").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) { return false; }
    }

    @Override public void close() {}

    // ── Condition → Solr Query Syntax ────────────────────────────────────────

    private String buildSolrQuery(Condition cond, String alias) {
        return switch (cond) {
            case Condition.And a -> {
                String l = buildSolrQuery(a.left(), alias);
                String r = buildSolrQuery(a.right(), alias);
                yield l.isBlank() || r.isBlank() ? l + r : "(" + l + " AND " + r + ")";
            }
            case Condition.Or o -> {
                String l = buildSolrQuery(o.left(), alias);
                String r = buildSolrQuery(o.right(), alias);
                yield l.isBlank() || r.isBlank() ? "" : "(" + l + " OR " + r + ")";
            }
            case Condition.Not n -> {
                String inner = buildSolrQuery(n.inner(), alias);
                yield inner.isBlank() ? "" : "NOT (" + inner + ")";
            }
            case Condition.Compare c -> buildSolrCompare(c, alias);
            case Condition.In i -> {
                if (!(i.expr() instanceof Expr.Column col)) yield "";
                if (isOtherAlias(col, alias)) yield "";
                String vals = i.values().stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(" "));
                yield col.name() + ":(" + vals + ")";
            }
            case Condition.IsNull isn -> {
                if (!(isn.expr() instanceof Expr.Column col)) yield "";
                if (isOtherAlias(col, alias)) yield "";
                yield "-" + col.name() + ":[* TO *]";
            }
            case Condition.Like like -> {
                if (!(like.expr() instanceof Expr.Column col)) yield "";
                if (isOtherAlias(col, alias)) yield "";
                String pattern = like.pattern().replace('%', '*').replace('_', '?');
                yield col.name() + ":" + pattern;
            }
            case Condition.Between between -> {
                if (!(between.expr() instanceof Expr.Column col)) yield "";
                if (isOtherAlias(col, alias)) yield "";
                yield col.name() + ":[" + between.low() + " TO " + between.high() + "]";
            }
        };
    }

    private boolean isOtherAlias(Expr.Column col, String alias) {
        return alias != null && col.tableAlias() != null && !col.tableAlias().equals(alias);
    }

    private String buildSolrCompare(Condition.Compare c, String alias) {
        if (!(c.left() instanceof Expr.Column col) || !(c.right() instanceof Expr.Literal lit)) return "";
        if (isOtherAlias(col, alias)) return "";
        String f = col.name();
        Object v = lit.value();
        return switch (c.op()) {
            case "="        -> f + ":\"" + v + "\"";
            case "!=", "<>" -> "NOT " + f + ":\"" + v + "\"";
            case ">"        -> f + ":{" + v + " TO *}";
            case ">="       -> f + ":[" + v + " TO *]";
            case "<"        -> f + ":{* TO " + v + "}";
            case "<="       -> f + ":[* TO " + v + "]";
            default         -> "";
        };
    }

    private String buildFieldList(LocalSelectQuery query) {
        if (query.star() || query.projections().isEmpty()) return "";
        return query.projections().stream().map(ColumnRef::column).collect(Collectors.joining(","));
    }

}
