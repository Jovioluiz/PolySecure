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
            case InsertStatement i       -> requirePolystoreTable(i.tableName());
            case InsertSelectStatement is -> {
                requirePolystoreTable(is.tableName());
                validateSelect(is.source());
            }
            case UpdateStatement u       -> requirePolystoreTable(u.tableName());
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
