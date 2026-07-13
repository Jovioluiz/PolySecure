/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.config.PersistenceService;
import com.polysecure.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DdlExecutor {

    private final MetadataCatalog catalog;
    private final StoreRegistry registry;
    private final TransactionCoordinator tx;
    private final PersistenceService persistence;

    public DdlExecutor(MetadataCatalog catalog, StoreRegistry registry,
                       TransactionCoordinator tx, PersistenceService persistence) {
        this.catalog = catalog;
        this.registry = registry;
        this.tx = tx;
        this.persistence = persistence;
    }

    public DmlResult createTable(CreateTableStatement stmt) {
        // Validate all referenced stores are registered
        Set<String> stores = stmt.columns().stream().map(ColumnDefinition::store).collect(Collectors.toSet());
        stores.forEach(s -> registry.get(s)); // throws if not found

        List<TransactionCoordinator.Operation> ops = new ArrayList<>();
        for (String store : stores) {
            List<ColumnDefinition> cols = stmt.columns().stream()
                .filter(c -> c.store().equals(store))
                .collect(Collectors.toList());

            ops.add(new TransactionCoordinator.Operation(
                "CREATE TABLE " + stmt.tableName() + " IN " + store,
                () -> registry.get(store).createTable(stmt.tableName(), cols),
                () -> registry.get(store).dropTable(stmt.tableName())
            ));
        }

        // Register in catalog first so rollback can reference it
        tx.run(ops);
        catalog.register(stmt);
        persistence.saveCatalog();

        return new DmlResult("CREATE", stmt.tableName(),
            stores.stream().collect(Collectors.toMap(s -> s, s -> 0)),
            "Table '" + stmt.tableName() + "' created in stores: " + String.join(", ", stores));
    }

    public DmlResult dropTable(DropTableStatement stmt) {
        Set<String> stores = catalog.getStores(stmt.tableName());

        List<TransactionCoordinator.Operation> ops = stores.stream()
            .map(store -> new TransactionCoordinator.Operation(
                "DROP TABLE " + stmt.tableName() + " IN " + store,
                () -> registry.get(store).dropTable(stmt.tableName()),
                () -> {} // no clean rollback for drop
            ))
            .collect(Collectors.toList());

        tx.run(ops);
        catalog.unregister(stmt.tableName());
        persistence.saveCatalog();

        return new DmlResult("DROP", stmt.tableName(),
            stores.stream().collect(Collectors.toMap(s -> s, s -> 0)),
            "Table '" + stmt.tableName() + "' dropped from stores: " + String.join(", ", stores));
    }

    public DmlResult createIndex(CreateIndexStatement stmt) {
        registry.get(stmt.storeName()).createIndex(stmt.table(), stmt.indexName(), stmt.columns());
        return new DmlResult("CREATE_INDEX", stmt.table(),
            Map.of(stmt.storeName(), 0),
            "Index '" + stmt.indexName() + "' created on " + stmt.storeName() + "." + stmt.table());
    }

    public DmlResult alterTable(AlterTableStatement stmt) {
        if (stmt.operation() == AlterTableStatement.AlterOp.ADD_COLUMN) {
            ColumnDefinition col = stmt.addColumn();
            registry.get(col.store()).addColumn(stmt.tableName(), col);
            if (catalog.exists(stmt.tableName())) {
                catalog.addColumn(stmt.tableName(), col);
                persistence.saveCatalog();
            }
            return new DmlResult("ALTER_TABLE", stmt.tableName(),
                Map.of(col.store(), 0),
                "Column '" + col.name() + "' added to '" + stmt.tableName() + "' in store " + col.store());
        } else {
            registry.get(stmt.dropStoreName()).dropColumn(stmt.tableName(), stmt.dropColumnName());
            if (catalog.exists(stmt.tableName())) {
                catalog.removeColumn(stmt.tableName(), stmt.dropColumnName(), stmt.dropStoreName());
                persistence.saveCatalog();
            }
            return new DmlResult("ALTER_TABLE", stmt.tableName(),
                Map.of(stmt.dropStoreName(), 0),
                "Column '" + stmt.dropColumnName() + "' dropped from '" + stmt.tableName()
                    + "' in store " + stmt.dropStoreName());
        }
    }
}
