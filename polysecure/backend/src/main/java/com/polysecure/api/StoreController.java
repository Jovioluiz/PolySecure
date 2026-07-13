/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.api;

import com.polysecure.api.dto.RegisterStoreRequest;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.config.PersistenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/stores")
public class StoreController {

    private final StoreRegistry registry;
    private final PersistenceService persistence;

    public StoreController(StoreRegistry registry, PersistenceService persistence) {
        this.registry = registry;
        this.persistence = persistence;
    }

    @GetMapping
    public Collection<String> list() {
        return registry.listNames();
    }

    @PostMapping
    public ResponseEntity<String> register(@RequestBody RegisterStoreRequest req) {
        registry.register(new StoreConfig(
            req.name(), req.type(), req.host(), req.port(),
            req.database(), req.username(), req.password()
        ));
        persistence.saveStores();
        return ResponseEntity.ok("Store '" + req.name() + "' registered.");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> unregister(@PathVariable String name) {
        registry.unregister(name);
        persistence.saveStores();
        return ResponseEntity.ok("Store '" + name + "' removed.");
    }
}
