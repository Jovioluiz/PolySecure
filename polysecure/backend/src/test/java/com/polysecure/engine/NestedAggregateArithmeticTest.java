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
import org.assertj.core.data.Offset;
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
 * Regression coverage for an aggregate function nested inside arithmetic in the SELECT list
 * (e.g. "(SUM(a) + SUM(b)) / SUM(c)"), across a cross-store JOIN, with and without GROUP BY.
 * Uses two embedded SQLite files as stand-ins for the two JDBC stores in the reported query —
 * exercises the same QueryEngine code path as any other JDBC-backed adapter without requiring
 * Docker/Testcontainers.
 *
 * <p>Bug: {@link ColumnRef#isAggregate()} only recognizes a <em>bare</em> aggregate
 * ("SUM(x)") — it's false when the top-level projection expression is
 * {@code Expr.Arithmetic} wrapping aggregates. {@code QueryEngine.executeSelect}'s
 * {@code hasAggregates} check and {@code SemanticValidator.validateGroupBy}'s equivalent both
 * used {@code isAggregate()}, so a query like the one below silently skipped
 * {@code applyGroupBy()} entirely and fell through to per-row projection: each aggregate leaf
 * resolved against a single joined row (via the row's absent {@code "SUM(x)"} key) instead of
 * reducing over the group, so every row came back with the same {@code null} computed value —
 * rows are returned, but the computed column is always empty. Fixed by
 * {@link ColumnRef#containsAggregate()}, which recurses through {@code Expr.Arithmetic}.
 */
class NestedAggregateArithmeticTest {

    static QueryEngine engine;

    @BeforeAll
    static void setup() throws Exception {
        String db1 = Files.createTempFile("nested-agg-1", ".db").toString();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db1);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE produtos (id INTEGER PRIMARY KEY, preco REAL, estoque INTEGER, categoria TEXT)");
            stmt.execute("INSERT INTO produtos (id, preco, estoque, categoria) VALUES (1, 600.0, 150, 'a')");
            stmt.execute("INSERT INTO produtos (id, preco, estoque, categoria) VALUES (2, 700.0, 200, 'a')");
            stmt.execute("INSERT INTO produtos (id, preco, estoque, categoria) VALUES (3, 100.0, 50, 'b')"); // filtered out by WHERE
        }
        String db2 = Files.createTempFile("nested-agg-2", ".db").toString();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db2);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE fb_produtos (id INTEGER PRIMARY KEY, valor REAL)");
            stmt.execute("INSERT INTO fb_produtos (id, valor) VALUES (1, 900.0)");
            stmt.execute("INSERT INTO fb_produtos (id, valor) VALUES (2, 800.0)");
            stmt.execute("INSERT INTO fb_produtos (id, valor) VALUES (3, 50.0)");
        }

        AdapterFactory factory = new AdapterFactory();
        MetadataCatalog catalog = new MetadataCatalog();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        StoreRegistry registry = new StoreRegistry(factory, meterRegistry, observationRegistry);
        registry.register(new StoreConfig("s1", StoreType.SQLITE, null, 0, db1, null, null));
        registry.register(new StoreConfig("s2", StoreType.SQLITE, null, 0, db2, null, null));

        TransactionCoordinator tx = new TransactionCoordinator();
        LinearCostModel costModel = new LinearCostModel();
        com.polysecure.config.PersistenceService persistence = new com.polysecure.config.PersistenceService(
            registry, catalog, costModel, System.getProperty("java.io.tmpdir") + "/nested-agg-arith-persist");
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
    private List<Map<String, Object>> select(String sql) {
        return (List<Map<String, Object>>) engine.execute(sql);
    }

    @Test
    void nestedAggregateInArithmeticWithoutGroupBy_crossStoreJoin() {
        var rows = select("""
            SELECT (sum(fp.valor) + sum(mp.preco)) / sum(mp.estoque)
            FROM s1.produtos mp
            JOIN s2.fb_produtos fp ON mp.id = fp.id
            WHERE fp.valor > 500
            AND mp.preco > 500
            AND mp.estoque > 100
            """);
        // Standard SQL: a bare aggregate expression with no GROUP BY collapses to exactly
        // one row (the whole filtered result set is a single implicit group) — not one row
        // per matched join pair.
        assertThat(rows).hasSize(1);
        Object val = rows.get(0).values().iterator().next();
        // (900+800 + 600+700) / (150+200) = 3000 / 350 = 8.571...
        assertThat(val).isNotNull();
        assertThat(((Number) val).doubleValue()).isCloseTo(3000.0 / 350.0, Offset.offset(0.001));
    }

    @Test
    void nestedAggregateInArithmeticWithGroupBy() {
        var rows = select("""
            SELECT mp.categoria, sum(mp.preco) / sum(mp.estoque) AS avgPreco
            FROM s1.produtos mp
            GROUP BY mp.categoria
            """);
        assertThat(rows).hasSize(2);
        Map<String, Object> catA = rows.stream()
            .filter(r -> "a".equals(r.get("categoria"))).findFirst().orElseThrow();
        // category 'a': (600+700) / (150+200) = 1300 / 350
        assertThat(((Number) catA.get("avgPreco")).doubleValue())
            .isCloseTo(1300.0 / 350.0, Offset.offset(0.001));
    }
}
