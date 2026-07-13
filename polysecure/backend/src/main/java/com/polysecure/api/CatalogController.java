/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.api;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.model.ColumnDefinition;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final MetadataCatalog catalog;
    private final StoreRegistry storeRegistry;

    public CatalogController(MetadataCatalog catalog, StoreRegistry storeRegistry) {
        this.catalog = catalog;
        this.storeRegistry = storeRegistry;
    }

    @GetMapping("/tables")
    public Map<String, Object> listTables() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String table : catalog.listTables()) {
            List<Map<String, Object>> cols = catalog.getColumns(table).stream()
                .map(c -> {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("name", c.name());
                    col.put("type", c.type());
                    col.put("store", c.store());
                    col.put("primaryKey", c.primaryKey());
                    return col;
                })
                .collect(Collectors.toList());
            result.put(table, cols);
        }
        return result;
    }

    @GetMapping("/stores")
    public List<String> listStores() {
        return new ArrayList<>(storeRegistry.listNames());
    }
}
