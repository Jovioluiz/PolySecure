package com.polysecure.engine;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.model.CreateTableStatement;
import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.DropTableStatement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DdlExecutor {

    private final MetadataCatalog catalog;
    private final StoreRegistry registry;
    private final TransactionCoordinator tx;

    public DdlExecutor(MetadataCatalog catalog, StoreRegistry registry, TransactionCoordinator tx) {
        this.catalog = catalog;
        this.registry = registry;
        this.tx = tx;
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

        return new DmlResult("DROP", stmt.tableName(),
            stores.stream().collect(Collectors.toMap(s -> s, s -> 0)),
            "Table '" + stmt.tableName() + "' dropped from stores: " + String.join(", ", stores));
    }
}
