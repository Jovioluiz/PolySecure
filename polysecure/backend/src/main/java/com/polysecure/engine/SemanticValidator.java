/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
