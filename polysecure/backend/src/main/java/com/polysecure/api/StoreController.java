package com.polysecure.api;

import com.polysecure.api.dto.RegisterStoreRequest;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.catalog.StoreRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/stores")
public class StoreController {

    private final StoreRegistry registry;

    public StoreController(StoreRegistry registry) {
        this.registry = registry;
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
        return ResponseEntity.ok("Store '" + req.name() + "' registered.");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> unregister(@PathVariable String name) {
        registry.unregister(name);
        return ResponseEntity.ok("Store '" + name + "' removed.");
    }
}
