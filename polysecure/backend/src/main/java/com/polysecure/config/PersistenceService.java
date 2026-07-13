/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.engine.LinearCostModel;
import com.polysecure.model.ColumnDefinition;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Persists StoreRegistry configs, MetadataCatalog schemas, and LinearCostModel weights
 * to disk so they survive server restarts.
 */
@Service
public class PersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);

    private final StoreRegistry registry;
    private final MetadataCatalog catalog;
    private final LinearCostModel costModel;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${polysecure.data.dir:./data}")
    private String dataDir;

    @org.springframework.beans.factory.annotation.Autowired
    public PersistenceService(StoreRegistry registry, MetadataCatalog catalog, LinearCostModel costModel) {
        this.registry = registry;
        this.catalog = catalog;
        this.costModel = costModel;
    }

    // Secondary constructor for testing — accepts an explicit data directory
    public PersistenceService(StoreRegistry registry, MetadataCatalog catalog,
                              LinearCostModel costModel, String dataDir) {
        this.registry = registry;
        this.catalog = catalog;
        this.costModel = costModel;
        this.dataDir = dataDir;
    }

    @PostConstruct
    public void load() {
        File dir = new File(dataDir);
        if (!dir.exists()) dir.mkdirs();

        loadStores(new File(dir, "stores.json"));
        loadCatalog(new File(dir, "catalog.json"));
        loadCostModel(new File(dir, "cost_model.json"));
    }

    @PreDestroy
    public void saveAll() {
        saveCostModel();
    }

    // ── Stores ───────────────────────────────────────────────────────────────

    public void saveStores() {
        File file = new File(dataDir, "stores.json");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, registry.getConfigs());
            log.debug("stores persisted to {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("failed to persist stores: {}", e.getMessage());
        }
    }

    private void loadStores(File file) {
        if (!file.exists()) return;
        try {
            List<StoreConfig> configs = mapper.readValue(file, new TypeReference<>() {});
            int count = 0;
            for (StoreConfig config : configs) {
                try {
                    registry.register(config);
                    count++;
                } catch (Exception e) {
                    log.warn("could not restore store '{}': {}", config.name(), e.getMessage());
                }
            }
            log.info("restored {} store(s) from {}", count, file.getAbsolutePath());
        } catch (IOException e) {
            log.warn("could not load stores.json: {}", e.getMessage());
        }
    }

    // ── Catalog ──────────────────────────────────────────────────────────────

    public void saveCatalog() {
        File file = new File(dataDir, "catalog.json");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, catalog.dump());
            log.debug("catalog persisted to {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("failed to persist catalog: {}", e.getMessage());
        }
    }

    private void loadCatalog(File file) {
        if (!file.exists()) return;
        try {
            Map<String, List<ColumnDefinition>> data =
                mapper.readValue(file, new TypeReference<>() {});
            catalog.loadRaw(data);
            log.info("restored {} polystore table(s) from {}", data.size(), file.getAbsolutePath());
        } catch (IOException e) {
            log.warn("could not load catalog.json: {}", e.getMessage());
        }
    }

    // ── Cost model ───────────────────────────────────────────────────────────

    public void saveCostModel() {
        File file = new File(dataDir, "cost_model.json");
        try {
            CostModelSnapshot snapshot = new CostModelSnapshot(
                costModel.weights(), costModel.sampleCount());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, snapshot);
            log.debug("cost model weights persisted ({} samples)", snapshot.sampleCount());
        } catch (IOException e) {
            log.error("failed to persist cost model: {}", e.getMessage());
        }
    }

    private void loadCostModel(File file) {
        if (!file.exists()) return;
        try {
            CostModelSnapshot snapshot = mapper.readValue(file, CostModelSnapshot.class);
            costModel.loadWeights(snapshot.weights(), snapshot.sampleCount());
            log.info("restored cost model weights ({} samples)", snapshot.sampleCount());
        } catch (IOException e) {
            log.warn("could not load cost_model.json: {}", e.getMessage());
        }
    }

    private record CostModelSnapshot(double[] weights, long sampleCount) {}
}
