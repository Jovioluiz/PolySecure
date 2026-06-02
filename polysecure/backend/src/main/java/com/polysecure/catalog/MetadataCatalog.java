package com.polysecure.catalog;

import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.CreateTableStatement;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MetadataCatalog {

    private final Map<String, List<ColumnDefinition>> tables = new ConcurrentHashMap<>();

    public void register(CreateTableStatement stmt) {
        if (tables.containsKey(stmt.tableName())) {
            throw new IllegalStateException("Table already exists: " + stmt.tableName());
        }
        tables.put(stmt.tableName(), List.copyOf(stmt.columns()));
    }

    public void unregister(String tableName) {
        if (!tables.containsKey(tableName)) {
            throw new IllegalArgumentException("Table not found: " + tableName);
        }
        tables.remove(tableName);
    }

    public List<ColumnDefinition> getColumns(String tableName) {
        List<ColumnDefinition> cols = tables.get(tableName);
        if (cols == null) throw new IllegalArgumentException("Table not found: " + tableName);
        return cols;
    }

    public Set<String> getStores(String tableName) {
        return getColumns(tableName).stream()
            .map(ColumnDefinition::store)
            .collect(Collectors.toSet());
    }

    public List<ColumnDefinition> getColumnsForStore(String tableName, String store) {
        return getColumns(tableName).stream()
            .filter(c -> c.store().equals(store))
            .collect(Collectors.toList());
    }

    public Optional<ColumnDefinition> getPrimaryKey(String tableName) {
        return getColumns(tableName).stream()
            .filter(ColumnDefinition::primaryKey)
            .findFirst();
    }

    public boolean exists(String tableName) {
        return tables.containsKey(tableName);
    }

    public Set<String> listTables() {
        return Collections.unmodifiableSet(tables.keySet());
    }
}
