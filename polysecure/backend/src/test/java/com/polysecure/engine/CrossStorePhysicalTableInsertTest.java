/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.adapter.AdapterFactory;
import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreConfig;
import com.polysecure.catalog.StoreConfig.StoreType;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.parser.SqlPolyFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage: {@code INSERT INTO POLYSTORE} and {@code UPDATE POLYSTORE} work directly
 * against pre-existing physical tables in each store — no {@code CREATE POLYSTORE TABLE} needed
 * — including when those physical tables have different native names per store (e.g. "produtos"
 * in one store, "FB_PRODUTOS" in another), via the "store.physical_table(...)" override on
 * INSERT's storeInsertClause and the equivalent "store.physicalTable.col = value" on UPDATE's
 * setClause.
 */
class CrossStorePhysicalTableInsertTest {

    static QueryEngine engine;
    static String db1;
    static String db2;

    @BeforeAll
    static void setup() throws Exception {
        db1 = Files.createTempFile("cross-insert-1", ".db").toString();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db1);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE produtos (id INTEGER PRIMARY KEY, preco REAL)");
        }
        db2 = Files.createTempFile("cross-insert-2", ".db").toString();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db2);
             Statement stmt = conn.createStatement()) {
            // Different physical name from the logical polystore table ("produtos").
            stmt.execute("CREATE TABLE FB_PRODUTOS (id INTEGER PRIMARY KEY, valor REAL)");
        }

        AdapterFactory factory = new AdapterFactory();
        MetadataCatalog catalog = new MetadataCatalog();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        StoreRegistry registry = new StoreRegistry(factory, meterRegistry, observationRegistry);
        registry.register(new StoreConfig("mariadb", StoreType.SQLITE, null, 0, db1, null, null));
        registry.register(new StoreConfig("fb", StoreType.SQLITE, null, 0, db2, null, null));

        TransactionCoordinator tx = new TransactionCoordinator();
        LinearCostModel costModel = new LinearCostModel();
        com.polysecure.config.PersistenceService persistence = new com.polysecure.config.PersistenceService(
            registry, catalog, costModel, System.getProperty("java.io.tmpdir") + "/cross-insert-persist");
        DdlExecutor ddl = new DdlExecutor(catalog, registry, tx, persistence);
        DmlExecutor dml = new DmlExecutor(catalog, registry, tx);
        MaterializedViewCache viewCache = new MaterializedViewCache(meterRegistry);
        StatsRegistry stats = new StatsRegistry(registry);
        CostEstimator cost = new CostEstimator(stats, costModel);
        SemanticValidator validator = new SemanticValidator(registry, catalog);
        engine = new QueryEngine(new SqlPolyFacade(), registry, ddl, dml, viewCache, cost, validator, persistence,
            observationRegistry);
    }

    @SuppressWarnings("unchecked")
    private Object execute(String sql) {
        return engine.execute(sql);
    }

    @Test
    void insertIntoPolystoreTargetsPhysicalTablesWithNoPriorCreatePolystoreTable() {
        // No "CREATE POLYSTORE TABLE produtos (...)" ever ran — this must still work directly
        // against the physical tables, and "fb.FB_PRODUTOS" must resolve to the FB_PRODUTOS
        // table rather than a table literally named "produtos" (which doesn't exist in "fb").
        execute("""
            INSERT INTO POLYSTORE produtos
                mariadb (id, preco) VALUES (1, 10.0)
                fb.FB_PRODUTOS (id, valor) VALUES (1, 20.0)
            """);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db1);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT preco FROM produtos WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble("preco")).isEqualTo(10.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db2);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT valor FROM FB_PRODUTOS WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble("valor")).isEqualTo(20.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void insertSelectIntoPolystoreTargetsPhysicalTablesWithDifferentNames() throws Exception {
        // Self-contained: seeds its own source row (id=2) rather than depending on the other
        // test's insert, since JUnit doesn't guarantee test method execution order.
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db1);
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO produtos (id, preco) VALUES (2, 30.0)");
        }

        execute("""
            INSERT INTO POLYSTORE produtos
                fb.FB_PRODUTOS (id, valor)
            SELECT id, preco AS valor FROM mariadb.produtos WHERE id = 2
            """);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db2);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT valor FROM FB_PRODUTOS WHERE id = 2")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble("valor")).isEqualTo(30.0);
        }
    }

    @Test
    void updatePolystoreTargetsPhysicalTablesWithDifferentNames() throws Exception {
        // Self-contained: seeds its own row (id=3) rather than depending on other tests.
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db1);
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO produtos (id, preco) VALUES (3, 1.0)");
        }
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db2);
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO FB_PRODUTOS (id, valor) VALUES (3, 1.0)");
        }

        // "mariadb.preco" has no override (defaults to logical "produtos"); "fb.FB_PRODUTOS.valor"
        // overrides the physical table, same syntax as storeInsertClause's "store.table(...)".
        execute("""
            UPDATE POLYSTORE produtos
                SET mariadb.preco = 199.9, fb.FB_PRODUTOS.valor = 655.99
                WHERE id = 3
            """);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db1);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT preco FROM produtos WHERE id = 3")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble("preco")).isEqualTo(199.9);
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db2);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT valor FROM FB_PRODUTOS WHERE id = 3")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble("valor")).isEqualTo(655.99);
        }
    }
}
