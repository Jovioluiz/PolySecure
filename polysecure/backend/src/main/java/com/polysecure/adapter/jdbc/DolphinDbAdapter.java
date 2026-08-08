/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.jdbc;

import com.polysecure.adapter.StoreCapabilities;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.Condition;
import com.polysecure.model.LocalSelectQuery;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DolphinDbAdapter extends JdbcStoreAdapter {

    private final String databasePath;
    // Idempotency guard so ensureLoaded() only pays the round-trip once per table per adapter
    // lifetime, not on every single/select/insert/update/delete call.
    private final Set<String> loadAttempted = ConcurrentHashMap.newKeySet();

    // Pool pinned to 1 connection: createTable()'s plain (unshared) DolphinDB table is only
    // visible on the session that created it, so every statement must reuse that same session.
    public DolphinDbAdapter(StoreConfig config) {
        super(config.name(),
            "jdbc:dolphindb://" + config.host() + ":" + config.port(),
            config.username(), config.password(), Map.of(), 1);
        this.databasePath = config.database();
    }

    // Tables that live on disk (created outside PolySecure via database()+createTable/saveTable,
    // or surviving a server restart) are NOT visible to a fresh session as bare names — DolphinDB
    // resolves unqualified identifiers only against in-session variables and shared globals, so
    // "SELECT * FROM t1" fails with "Can't find the object with name t1" until the table has been
    // loadTable()'d and shared at least once. This makes that happen lazily and once per table.
    //
    // Deliberately NOT wrapped in a DolphinDB-side `if(existsDatabase(...) and existsTable(...))`
    // guard: through this driver, wrapping the share statement inside an if{} block (with or
    // without a nested try{}catch(){}) makes jdbc.execute() report success while the share
    // silently never happens — reproduced and confirmed against a live server, cause unclear
    // (suspect a driver-side script-batching quirk specific to multi-statement/braced scripts).
    // A bare top-level `share loadTable(...) as table;` does not have this problem. The downside
    // is a failed round-trip (caught below) for tables that aren't on-disk objects at all — a
    // one-time cost per table thanks to loadAttempted.
    private void ensureLoaded(String table) {
        if (databasePath == null || databasePath.isBlank() || !loadAttempted.add(table)) return;
        String path = databasePath.replace('\\', '/');
        String script = "share loadTable(\"" + path + "\", \"" + table + "\") as " + table + ";";
        try {
            jdbc.execute(script);
        } catch (Exception ignored) {
            // best-effort: table isn't an on-disk object at this path — subsequent SQL surfaces
            // its own clear error if `table` doesn't resolve any other way either
        }
    }

    // Probes whether `table` resolves to *some* object in the current session (in-memory,
    // on-disk-and-loaded, or shared) — used to decide whether createTable() should skip issuing
    // CREATE TABLE (which would otherwise define a new, empty, session-local table that shadows
    // any already-loaded on-disk table of the same name). Uses queryForList rather than
    // queryForObject(..., Long.class): DolphinDB's count(*) comes back as a plain Integer for
    // small tables, and Spring would throw ClassCastException trying to coerce it to Long.
    private boolean tableResolvable(String table) {
        try {
            jdbc.queryForList("select count(*) from " + table);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> select(LocalSelectQuery query) {
        ensureLoaded(query.table());
        return super.select(query);
    }

    @Override
    public void insert(String table, Map<String, Object> values) {
        ensureLoaded(table);
        super.insert(table, values);
    }

    @Override
    public int update(String table, Map<String, Object> updates, Condition where) {
        ensureLoaded(table);
        return super.update(table, updates, where);
    }

    @Override
    public int delete(String table, Condition where) {
        ensureLoaded(table);
        return super.delete(table, where);
    }

    // DolphinDB's SQL dialect has no CREATE TABLE IF NOT EXISTS — tolerate "already exists".
    // Also: if the table already exists physically in the configured on-disk database, load and
    // share it instead of issuing CREATE TABLE — a plain CREATE TABLE would otherwise define a
    // brand-new, empty, session-local table that shadows the real on-disk data under the same name.
    @Override
    public void createTable(String table, List<ColumnDefinition> columns) {
        ensureLoaded(table);
        if (tableResolvable(table)) return;
        String cols = columns.stream()
            .map(c -> c.name() + " " + toSqlType(c.type()))
            .collect(Collectors.joining(", "));
        try {
            jdbc.execute("CREATE TABLE " + table + " (" + cols + ")");
        } catch (Exception e) {
            if (!e.getMessage().toLowerCase().contains("already exists")) throw e;
        }
    }

    @Override
    public void dropTable(String table) {
        try {
            jdbc.execute("DROP TABLE " + table);
        } catch (Exception e) {
            // DolphinDB phrases this as "Table doesn't exist." — doesn't contain "not exist"
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (!msg.contains("not exist") && !msg.contains("doesn't exist")) throw e;
        }
    }

    @Override
    public long estimateCardinality(String table) {
        ensureLoaded(table);
        try {
            // count(*) comes back as a plain Integer for small tables — Number sidesteps the
            // ClassCastException that querying as Long.class would throw in that case.
            Number c = jdbc.queryForObject("SELECT count(*) FROM " + table, Number.class);
            return c != null ? c.longValue() : 0L;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    // No ANSI information_schema equivalent — schema inspection is not supported
    @Override
    public List<ColumnDefinition> getSchema(String table) { return List.of(); }

    @Override
    public StoreCapabilities getCapabilities() {
        // DDL/DML supported; distributed/partitioned tables have no classic multi-statement
        // transaction guarantees, same caveat as other columnar analytical stores.
        return new StoreCapabilities(true, true, true, true, true, false);
    }

    @Override
    protected String toSqlType(String type) {
        // DolphinDB has no auto-increment column — primary-key uniqueness is caller's responsibility
        return switch (type.toUpperCase()) {
            case "INT", "INTEGER"              -> "LONG";
            case "TEXT", "STRING", "VARCHAR"   -> "STRING";
            case "BOOLEAN", "BOOL"             -> "BOOL";
            case "FLOAT", "DOUBLE"             -> "DOUBLE";
            case "DOCUMENT"                    -> "STRING";
            default                            -> "STRING";
        };
    }
}
