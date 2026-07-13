/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.mongodb;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.polysecure.adapter.StoreAdapter;
import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

public class MongoAdapter implements StoreAdapter {

    private final String storeName;
    private final MongoClient client;
    private final MongoDatabase database;

    public MongoAdapter(StoreConfig config) {
        this.storeName = config.name();
        String uri = "mongodb://"
            + (config.username() != null && !config.username().isBlank()
               ? config.username() + ":" + config.password() + "@" : "")
            + config.host() + ":" + config.port();
        this.client = MongoClients.create(uri);
        this.database = client.getDatabase(config.database());
    }

    @Override
    public String storeName() { return storeName; }

    // ── SELECT ──────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        MongoCollection<Document> col = database.getCollection(query.table());
        Bson filter = query.where() != null ? toFilter(query.where(), query.alias()) : new Document();
        List<String> fields = projectedFields(query);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Document doc : col.find(filter)) {
            Map<String, Object> row = new LinkedHashMap<>();
            if (fields.isEmpty()) {
                doc.forEach((k, v) -> row.put(k, normalize(v)));
            } else {
                for (String f : fields) if (doc.containsKey(f)) row.put(f, normalize(doc.get(f)));
            }
            results.add(row);
        }
        return results;
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        // MongoDB is schema-less — just ensure the collection exists
        if (!collectionExists(table)) database.createCollection(table);
    }

    @Override
    public void dropTable(String table) {
        database.getCollection(table).drop();
    }

    // ── DML ─────────────────────────────────────────────────────────────────

    @Override
    public void insert(String table, Map<String, Object> values) {
        database.getCollection(table).insertOne(new Document(values));
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        Bson filter = where != null ? toFilter(where, null) : new Document();
        List<Bson> sets = updates.entrySet().stream()
            .map(e -> Updates.set(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
        var result = database.getCollection(table).updateMany(filter, Updates.combine(sets));
        return (int) result.getModifiedCount();
    }

    @Override
    public int delete(String table, Condition where) {
        Bson filter = where != null ? toFilter(where, null) : new Document();
        var result = database.getCollection(table).deleteMany(filter);
        return (int) result.getDeletedCount();
    }

    @Override
    public long estimateCardinality(String table) {
        try {
            return database.getCollection(table).estimatedDocumentCount();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public StoreCapabilities getCapabilities() {
        // MongoDB is schema-less — DDL is limited (no ALTER, no constraints beyond simple ones)
        return new StoreCapabilities(true, true, true, true, false, false);
    }

    @Override
    public boolean ping() {
        try { database.runCommand(new Document("ping", 1)); return true; }
        catch (Exception e) { return false; }
    }

    @Override
    public void close() { client.close(); }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private List<String> projectedFields(LocalSelectQuery query) {
        if (query.star() || query.projections().isEmpty()) return List.of();
        return query.projections().stream().map(ColumnRef::column).collect(Collectors.toList());
    }

    private Object normalize(Object v) {
        if (v != null && v.getClass().getSimpleName().equals("ObjectId")) return v.toString();
        return v;
    }

    private boolean collectionExists(String name) {
        for (String c : database.listCollectionNames()) if (c.equals(name)) return true;
        return false;
    }

    Bson toFilter(Condition cond, String alias) {
        return switch (cond) {
            case Condition.And a -> Filters.and(toFilter(a.left(), alias), toFilter(a.right(), alias));
            case Condition.Or  o -> Filters.or(toFilter(o.left(), alias), toFilter(o.right(), alias));
            case Condition.Not n -> Filters.nor(toFilter(n.inner(), alias));
            case Condition.Compare c -> toCompare(c, alias);
            case Condition.In i -> {
                if (!(i.expr() instanceof Expr.Column col)) yield new Document();
                yield Filters.in(col.name(), i.values());
            }
            case Condition.IsNull isn -> {
                if (!(isn.expr() instanceof Expr.Column col)) yield new Document();
                // $eq: null matches both null values and missing fields in MongoDB
                yield Filters.eq(col.name(), (Object) null);
            }
            case Condition.Like like -> {
                if (!(like.expr() instanceof Expr.Column col)) yield new Document();
                yield Filters.regex(col.name(), likeToRegex(like.pattern()));
            }
            case Condition.Between between -> {
                if (!(between.expr() instanceof Expr.Column col)) yield new Document();
                yield Filters.and(
                    Filters.gte(col.name(), between.low()),
                    Filters.lte(col.name(), between.high())
                );
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

    private Bson toCompare(Condition.Compare c, String alias) {
        if (!(c.left() instanceof Expr.Column col) || !(c.right() instanceof Expr.Literal lit)) {
            return new Document();
        }
        if (col.tableAlias() != null && alias != null && !col.tableAlias().equals(alias)) {
            return new Document();
        }
        String field = col.name();
        Object val = lit.value();
        return switch (c.op()) {
            case "="        -> Filters.eq(field, val);
            case "!=", "<>" -> Filters.ne(field, val);
            case ">"        -> Filters.gt(field, val);
            case "<"        -> Filters.lt(field, val);
            case ">="       -> Filters.gte(field, val);
            case "<="       -> Filters.lte(field, val);
            default -> throw new IllegalArgumentException("Unknown operator: " + c.op());
        };
    }
}
