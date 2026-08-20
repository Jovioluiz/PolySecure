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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression coverage for gap 3.3 (analise_gaps_implementacao.md): non-correlated subqueries
 * in WHERE — "expr IN (SELECT ...)"/"NOT IN (SELECT ...)" and scalar "expr &lt;op&gt;
 * (SELECT ...)". Uses two separate SQLite-backed stores (same pattern as
 * {@link CrossStorePhysicalTableInsertTest}) specifically to prove a subquery isn't
 * restricted to the outer query's own store — it's a fully independent nested SELECT that can
 * target any registered store.
 */
class SubqueryExecutionTest {

    static QueryEngine engine;
    static String ordersDb;
    static String customersDb;

    @BeforeAll
    static void setup() throws Exception {
        ordersDb = Files.createTempFile("subquery-orders", ".db").toString();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + ordersDb);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE orders (id INTEGER PRIMARY KEY, customer_id INTEGER, total REAL)");
            stmt.execute("INSERT INTO orders VALUES (1, 100, 50.0)");
            stmt.execute("INSERT INTO orders VALUES (2, 200, 150.0)");
            stmt.execute("INSERT INTO orders VALUES (3, 300, 250.0)");
        }

        customersDb = Files.createTempFile("subquery-customers", ".db").toString();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + customersDb);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE vip_customers (id INTEGER PRIMARY KEY)");
            stmt.execute("INSERT INTO vip_customers VALUES (200)");
        }

        AdapterFactory factory = new AdapterFactory();
        MetadataCatalog catalog = new MetadataCatalog();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        StoreRegistry registry = new StoreRegistry(factory, meterRegistry, observationRegistry);
        registry.register(new StoreConfig("s1", StoreType.SQLITE, null, 0, ordersDb, null, null));
        registry.register(new StoreConfig("s2", StoreType.SQLITE, null, 0, customersDb, null, null));

        TransactionCoordinator tx = new TransactionCoordinator();
        LinearCostModel costModel = new LinearCostModel();
        com.polysecure.config.PersistenceService persistence = new com.polysecure.config.PersistenceService(
            registry, catalog, costModel, System.getProperty("java.io.tmpdir") + "/subquery-persist");
        DdlExecutor ddl = new DdlExecutor(catalog, registry, tx, persistence);
        DmlExecutor dml = new DmlExecutor(catalog, registry, tx);
        MaterializedViewCache viewCache = new MaterializedViewCache(meterRegistry);
        StatsRegistry stats = new StatsRegistry(registry);
        CostEstimator cost = new CostEstimator(stats, costModel);
        SemanticValidator validator = new SemanticValidator(registry, catalog);
        engine = new QueryEngine(new SqlPolyFacade(), registry, ddl, dml, viewCache, cost, validator, persistence,
            observationRegistry);

        // DELETE FROM POLYSTORE requires a registered polystore table (unlike UPDATE, which
        // can skip it when every SET clause is store-qualified) — createTable is
        // "IF NOT EXISTS" so this is a no-op against the already-seeded physical table above.
        engine.execute("""
            CREATE POLYSTORE TABLE orders (
                id INT STORE s1 PRIMARY KEY,
                customer_id INT STORE s1,
                total FLOAT STORE s1
            )
            """);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> select(String sql) {
        return (List<Map<String, Object>>) engine.execute(sql);
    }

    // SQLite's JDBC driver reports INTEGER columns as plain java.lang.Integer, not Long —
    // normalize before asserting so these tests don't depend on that driver-specific choice.
    private static long asLong(Object v) {
        return ((Number) v).longValue();
    }

    @Test
    void inSubqueryAcrossStoresReturnsOnlyMatchingRows() {
        // vip_customers lives in "s2", orders in "s1" — proves the subquery isn't restricted
        // to the outer query's own store.
        var rows = select(
            "SELECT id FROM s1.orders WHERE customer_id IN (SELECT id FROM s2.vip_customers)");
        assertThat(rows).extracting(r -> asLong(r.get("id"))).containsExactly(2L);
    }

    @Test
    void notInSubqueryExcludesMatchingRows() {
        var rows = select(
            "SELECT id FROM s1.orders WHERE customer_id NOT IN (SELECT id FROM s2.vip_customers)");
        assertThat(rows).extracting(r -> asLong(r.get("id"))).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void scalarSubqueryCompareReturnsRowsAboveAverage() {
        // AVG(total) over (50, 150, 250) = 150 — only the 250 order is strictly greater.
        var rows = select(
            "SELECT id FROM s1.orders WHERE total > (SELECT AVG(total) FROM s1.orders)");
        assertThat(rows).extracting(r -> asLong(r.get("id"))).containsExactly(3L);
    }

    @Test
    void emptySubqueryResultMakesInMatchNothing() {
        var rows = select(
            "SELECT id FROM s1.orders WHERE customer_id IN (SELECT id FROM s2.vip_customers WHERE id = 999999)");
        assertThat(rows).isEmpty();
    }

    @Test
    void scalarSubqueryWithMoreThanOneRowThrows() {
        assertThatThrownBy(() -> select(
            "SELECT id FROM s1.orders WHERE total > (SELECT total FROM s1.orders)"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Scalar subquery returned");
    }

    @Test
    void subqueryProjectingMoreThanOneColumnThrows() {
        assertThatThrownBy(() -> select(
            "SELECT id FROM s1.orders WHERE customer_id IN (SELECT id, total FROM s1.orders)"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("exactly one column");
    }

    @Test
    void deletePolystoreWithInSubqueryOnlyRemovesMatchingRows() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + ordersDb);
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO orders VALUES (4, 200, 999.0)");
        }
        engine.execute("DELETE FROM POLYSTORE orders WHERE customer_id IN (SELECT id FROM s2.vip_customers)");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + ordersDb);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) AS n FROM orders WHERE customer_id = 200")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("n")).isEqualTo(0);
        }
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + ordersDb);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) AS n FROM orders WHERE id IN (1, 3)")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("n")).isEqualTo(2);
        }
    }
}
