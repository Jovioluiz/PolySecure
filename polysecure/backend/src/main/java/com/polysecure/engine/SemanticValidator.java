/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates a parsed Statement before execution.
 * Checks that all store and polystore-table references are resolvable.
 */
@Component
public class SemanticValidator {

    private final StoreRegistry registry;
    private final MetadataCatalog catalog;

    public SemanticValidator(StoreRegistry registry, MetadataCatalog catalog) {
        this.registry = registry;
        this.catalog = catalog;
    }

    public void validate(Statement stmt) {
        switch (stmt) {
            case SelectStatement s       -> validateSelect(s);
            case InsertStatement i       -> validateInsertStores(i);
            case InsertSelectStatement is -> {
                validateInsertStores(is);
                validateSelect(is.source());
            }
            case UpdateStatement u       -> validateUpdate(u);
            case DeleteStatement d       -> requirePolystoreTable(d.tableName());
            case DropTableStatement d    -> requirePolystoreTable(d.tableName());
            case CreateTableStatement c  -> {
                if (catalog.exists(c.tableName())) {
                    throw new SemanticException("Table already exists: '" + c.tableName() + "'");
                }
                for (ColumnDefinition col : c.columns()) {
                    requireStore(col.store());
                }
            }
            case UnionStatement u -> {
                validateSelect(u.left());
                validateSelect(u.right());
            }
            case ExceptStatement e -> {
                validateSelect(e.left());
                validateSelect(e.right());
            }
            case RegisterStoreStatement r -> {
                if (registry.exists(r.name())) {
                    throw new SemanticException("Store already registered: '" + r.name() + "'");
                }
            }
            case CreateIndexStatement ci -> requireStore(ci.storeName());
            case AlterTableStatement at -> {
                if (at.operation() == AlterTableStatement.AlterOp.ADD_COLUMN) {
                    requireStore(at.addColumn().store());
                } else {
                    requireStore(at.dropStoreName());
                }
            }
        }
    }

    private void validateInsertStores(InsertStatement i) {
        i.clauses().forEach(c -> requireStore(c.storeName()));
    }

    private void validateInsertStores(InsertSelectStatement is) {
        is.targets().forEach(t -> requireStore(t.storeName()));
    }

    private void validateUpdate(UpdateStatement u) {
        boolean allQualified = u.sets().stream().allMatch(s -> s.store() != null);
        if (allQualified) {
            u.sets().stream().map(SetClause::store).forEach(this::requireStore);
        } else {
            requirePolystoreTable(u.tableName());
        }
    }

    private void validateSelect(SelectStatement s) {
        List<TableRef> refs = new ArrayList<>();
        refs.add(s.from());
        s.joins().forEach(j -> refs.add(j.table()));
        for (TableRef ref : refs) {
            String storeName = ref.store() != null ? ref.store() : ref.table();
            requireStore(storeName);
        }
        validateGroupBy(s);
        validateColumns(s);
    }

    /**
     * Checks every column referenced in the query against each store's real schema
     * (via StoreAdapter.getSchema), so a typo/renamed column fails fast with a clear
     * error instead of silently resolving to null downstream (empty projection, 0 for
     * SUM/AVG, single collapsed GROUP BY bucket).
     *
     * Skipped per-alias when the adapter can't report a schema (getSchema() returns
     * empty — e.g. MongoDB/Neo4j/Kafka, or a store lookup failure): there's nothing to
     * validate against, so we fail open rather than block schemaless stores.
     */
    private void validateColumns(SelectStatement s) {
        Map<String, TableRef> aliasTables = new LinkedHashMap<>();
        aliasTables.put(s.from().effectiveAlias(), s.from());
        for (JoinClause j : s.joins()) aliasTables.put(j.table().effectiveAlias(), j.table());

        Map<String, Set<String>> aliasColumns = new LinkedHashMap<>();
        for (Map.Entry<String, TableRef> entry : aliasTables.entrySet()) {
            TableRef ref = entry.getValue();
            String storeName = ref.store() != null ? ref.store() : ref.table();
            StoreAdapter adapter;
            try {
                adapter = registry.get(storeName);
            } catch (Exception ex) {
                adapter = null;
            }
            if (adapter == null) continue;
            List<ColumnDefinition> schema = adapter.getSchema(ref.table());
            if (!schema.isEmpty()) {
                aliasColumns.put(entry.getKey(),
                    schema.stream().map(ColumnDefinition::name).collect(Collectors.toSet()));
            }
        }
        if (aliasColumns.isEmpty()) return;

        List<Expr.Column> columnRefs = new ArrayList<>();
        for (ColumnRef proj : s.projections()) {
            if (proj.isStar()) continue;
            if (proj.isAggregate()) collectColumns(proj.aggExpr(), columnRefs);
            else columnRefs.add(new Expr.Column(proj.tableAlias(), proj.column()));
        }
        if (s.where() != null) collectColumns(s.where(), columnRefs);
        s.groupBy().forEach(e -> collectColumns(e, columnRefs));
        if (s.having() != null) collectColumns(s.having(), columnRefs);
        s.orderBy().forEach(o -> collectColumns(o.expr(), columnRefs));
        s.joins().forEach(j -> collectColumns(j.on(), columnRefs));

        for (Expr.Column col : columnRefs) {
            if (col.tableAlias() != null) {
                Set<String> known = aliasColumns.get(col.tableAlias());
                if (known != null && known.stream().noneMatch(c -> c.equalsIgnoreCase(col.name()))) {
                    throw unknownColumn(col.tableAlias() + "." + col.name(), aliasColumns);
                }
            } else if (aliasColumns.values().stream()
                    .noneMatch(set -> set.stream().anyMatch(c -> c.equalsIgnoreCase(col.name())))) {
                throw unknownColumn(col.name(), aliasColumns);
            }
        }
    }

    private SemanticException unknownColumn(String reference, Map<String, Set<String>> aliasColumns) {
        String available = aliasColumns.entrySet().stream()
            .map(e -> e.getKey() + "(" + e.getValue().stream().sorted().collect(Collectors.joining(", ")) + ")")
            .collect(Collectors.joining("; "));
        return new SemanticException(
            "Column '" + reference + "' does not exist. Available columns: " + available);
    }

    private void collectColumns(Expr e, List<Expr.Column> out) {
        switch (e) {
            case Expr.Column col -> out.add(col);
            case Expr.Aggregate agg -> collectColumns(agg.arg(), out);
            case Expr.Star ignored -> {}
            case Expr.Literal lit -> {}
        }
    }

    private void collectColumns(Condition cond, List<Expr.Column> out) {
        switch (cond) {
            case Condition.And a -> { collectColumns(a.left(), out); collectColumns(a.right(), out); }
            case Condition.Or o -> { collectColumns(o.left(), out); collectColumns(o.right(), out); }
            case Condition.Not n -> collectColumns(n.inner(), out);
            case Condition.Compare c -> { collectColumns(c.left(), out); collectColumns(c.right(), out); }
            case Condition.In i -> collectColumns(i.expr(), out);
            case Condition.IsNull isn -> collectColumns(isn.expr(), out);
            case Condition.Like like -> collectColumns(like.expr(), out);
            case Condition.Between b -> collectColumns(b.expr(), out);
        }
    }

    /**
     * Standard SQL rule: if the SELECT list mixes an aggregate function (COUNT/SUM/AVG/MIN/MAX)
     * with plain columns, every plain column must appear in GROUP BY (or be functionally
     * dependent on it). A bare aggregate with no other columns (e.g. SELECT COUNT(*) FROM t)
     * needs no GROUP BY — it's a single implicit group.
     */
    private void validateGroupBy(SelectStatement s) {
        boolean hasAggregate = s.projections().stream().anyMatch(ColumnRef::isAggregate);
        if (!hasAggregate) {
            return;
        }
        List<ColumnRef> plainColumns = s.projections().stream()
            .filter(c -> !c.isAggregate())
            .collect(Collectors.toList());
        if (plainColumns.isEmpty()) {
            return;
        }
        if (s.groupBy().isEmpty()) {
            String cols = plainColumns.stream()
                .map(c -> c.tableAlias() != null ? c.tableAlias() + "." + c.column() : c.column())
                .collect(Collectors.joining(", "));
            throw new SemanticException(
                "SELECT mixes aggregate functions with plain column(s) [" + cols
                + "] but has no GROUP BY. Add GROUP BY " + cols + ", or remove the plain column(s).");
        }
        for (ColumnRef proj : plainColumns) {
            boolean covered = s.groupBy().stream().anyMatch(e -> matchesColumn(e, proj));
            if (!covered) {
                String qualified = proj.tableAlias() != null ? proj.tableAlias() + "." + proj.column() : proj.column();
                throw new SemanticException(
                    "Column '" + qualified + "' must appear in the GROUP BY clause "
                    + "or be wrapped in an aggregate function.");
            }
        }
    }

    private boolean matchesColumn(Expr groupByExpr, ColumnRef proj) {
        if (!(groupByExpr instanceof Expr.Column col)) {
            return false;
        }
        if (!col.name().equals(proj.column())) {
            return false;
        }
        return col.tableAlias() == null || proj.tableAlias() == null
            || col.tableAlias().equals(proj.tableAlias());
    }

    private void requireStore(String name) {
        if (!registry.exists(name)) {
            throw new SemanticException("Store not registered: '" + name + "'");
        }
    }

    private void requirePolystoreTable(String name) {
        if (!catalog.exists(name)) {
            throw new SemanticException("Polystore table not found: '" + name + "'. "
                + "Use CREATE POLYSTORE TABLE first.");
        }
    }
}
