package com.polysecure.engine;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DmlExecutor {

    private final MetadataCatalog catalog;
    private final StoreRegistry registry;
    private final TransactionCoordinator tx;

    public DmlExecutor(MetadataCatalog catalog, StoreRegistry registry, TransactionCoordinator tx) {
        this.catalog = catalog;
        this.registry = registry;
        this.tx = tx;
    }

    // ── INSERT ───────────────────────────────────────────────────────────────

    public DmlResult insert(InsertStatement stmt) {
        List<TransactionCoordinator.Operation> ops = new ArrayList<>();
        Map<String, Integer> affected = new LinkedHashMap<>();

        for (StoreInsertClause clause : stmt.clauses()) {
            Map<String, Object> values = buildValueMap(clause.columns(), clause.values());
            ops.add(new TransactionCoordinator.Operation(
                "INSERT INTO " + stmt.tableName() + " IN " + clause.storeName(),
                () -> registry.get(clause.storeName()).insert(stmt.tableName(), values),
                () -> rollbackInsert(clause.storeName(), stmt.tableName(), values)
            ));
            affected.put(clause.storeName(), 1);
        }

        tx.run(ops);
        int total = affected.values().stream().mapToInt(Integer::intValue).sum();
        return new DmlResult("INSERT", stmt.tableName(), affected,
            "Inserted " + total + " row(s) across " + affected.size() + " store(s)");
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    public DmlResult update(UpdateStatement stmt) {
        // Group SET clauses by store
        Map<String, Map<String, Object>> updatesByStore = new LinkedHashMap<>();
        Set<String> tableStores = catalog.exists(stmt.tableName())
            ? catalog.getStores(stmt.tableName()) : Set.of();

        for (SetClause set : stmt.sets()) {
            Set<String> targets = set.store() != null ? Set.of(set.store()) : tableStores;
            for (String store : targets) {
                updatesByStore.computeIfAbsent(store, k -> new LinkedHashMap<>())
                    .put(set.column(), set.value());
            }
        }

        Map<String, Integer> affected = new LinkedHashMap<>();
        List<TransactionCoordinator.Operation> ops = updatesByStore.entrySet().stream()
            .map(e -> new TransactionCoordinator.Operation(
                "UPDATE " + stmt.tableName() + " IN " + e.getKey(),
                () -> {
                    int n = registry.get(e.getKey()).update(stmt.tableName(), e.getValue(), stmt.where());
                    affected.put(e.getKey(), n);
                },
                () -> {} // no clean rollback for updates without snapshot
            ))
            .collect(Collectors.toList());

        tx.run(ops);
        int total = affected.values().stream().mapToInt(Integer::intValue).sum();
        return new DmlResult("UPDATE", stmt.tableName(), affected,
            "Updated " + total + " row(s) across " + affected.size() + " store(s)");
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    public DmlResult delete(DeleteStatement stmt) {
        Set<String> stores = catalog.exists(stmt.tableName())
            ? catalog.getStores(stmt.tableName())
            : Set.of();

        if (stores.isEmpty()) {
            throw new IllegalArgumentException(
                "Table '" + stmt.tableName() + "' not found in catalog. "
                + "Use CREATE POLYSTORE TABLE first, or register it.");
        }

        Map<String, Integer> affected = new LinkedHashMap<>();
        List<TransactionCoordinator.Operation> ops = stores.stream()
            .map(store -> new TransactionCoordinator.Operation(
                "DELETE FROM " + stmt.tableName() + " IN " + store,
                () -> {
                    int n = registry.get(store).delete(stmt.tableName(), stmt.where());
                    affected.put(store, n);
                },
                () -> {} // no clean rollback for deletes
            ))
            .collect(Collectors.toList());

        tx.run(ops);
        int total = affected.values().stream().mapToInt(Integer::intValue).sum();
        return new DmlResult("DELETE", stmt.tableName(), affected,
            "Deleted " + total + " row(s) across " + affected.size() + " store(s)");
    }

    // ── INSERT … SELECT ──────────────────────────────────────────────────────

    public DmlResult insertFromSelect(InsertSelectStatement stmt, List<Map<String, Object>> rows) {
        Map<String, Integer> affected = new LinkedHashMap<>();
        List<TransactionCoordinator.Operation> ops = new ArrayList<>();

        for (StoreTargetClause target : stmt.targets()) {
            List<Map<String, Object>> storeRows = rows.stream()
                .map(row -> extractTargetColumns(row, target.columns(), target.storeName()))
                .collect(Collectors.toList());

            for (Map<String, Object> storeRow : storeRows) {
                Map<String, Object> snapshot = Map.copyOf(storeRow);
                ops.add(new TransactionCoordinator.Operation(
                    "INSERT INTO " + stmt.tableName() + " IN " + target.storeName(),
                    () -> registry.get(target.storeName()).insert(stmt.tableName(), snapshot),
                    () -> rollbackInsert(target.storeName(), stmt.tableName(), snapshot)
                ));
            }
            affected.put(target.storeName(), storeRows.size());
        }

        tx.run(ops);
        int total = affected.values().stream().mapToInt(Integer::intValue).sum();
        return new DmlResult("INSERT_SELECT", stmt.tableName(), affected,
            "Inserted " + rows.size() + " row(s) from SELECT into " + affected.size() + " store(s), "
            + total + " total insertion(s)");
    }

    private Map<String, Object> extractTargetColumns(Map<String, Object> row,
                                                      List<String> columns, String storeName) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String col : columns) {
            if (!row.containsKey(col)) {
                throw new IllegalArgumentException(
                    "SELECT result does not contain column '" + col + "' required by store '" + storeName + "'. "
                    + "Use AS aliases to map source column names to target column names.");
            }
            result.put(col, row.get(col));
        }
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildValueMap(List<String> cols, List<Object> vals) {
        if (cols.size() != vals.size()) {
            throw new IllegalArgumentException(
                "Column count (" + cols.size() + ") does not match value count (" + vals.size() + ")");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < cols.size(); i++) map.put(cols.get(i), vals.get(i));
        return map;
    }

    private void rollbackInsert(String store, String table, Map<String, Object> values) {
        // Try to delete the inserted row using a best-effort equality condition built from the values
        Optional<Map.Entry<String, Object>> pk = values.entrySet().stream()
            .filter(e -> e.getKey().equals("id") || e.getKey().endsWith("_id"))
            .findFirst();
        if (pk.isPresent()) {
            Condition cond = new Condition.Compare(
                new Expr.Column(null, pk.get().getKey()),
                "=",
                new Expr.Literal(pk.get().getValue())
            );
            registry.get(store).delete(table, cond);
        }
    }
}
