/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine.optimizer;

import com.polysecure.engine.CostEstimator;
import com.polysecure.engine.LinearCostModel;
import com.polysecure.engine.StatsRegistry;
import com.polysecure.model.Condition;
import com.polysecure.model.Expr;
import com.polysecure.model.JoinClause;
import com.polysecure.model.SelectStatement;
import com.polysecure.model.TableRef;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CalciteJoinOrderOptimizerTest {

    private final Map<String, Long> cardinalities = new HashMap<>();

    private CalciteJoinOrderOptimizer optimizer() {
        StatsRegistry stats = new StatsRegistry(null) {
            @Override
            public long cardinality(String storeName, String table) {
                return cardinalities.getOrDefault(storeName + "." + table, 1_000L);
            }
        };
        CostEstimator cost = new CostEstimator(stats, new LinearCostModel());
        return new CalciteJoinOrderOptimizer(cost);
    }

    private static TableRef table(String store, String tableName, String alias) {
        return new TableRef(store, tableName, alias);
    }

    private static Condition equi(String leftAlias, String leftCol, String rightAlias, String rightCol) {
        return new Condition.Compare(new Expr.Column(leftAlias, leftCol), "=", new Expr.Column(rightAlias, rightCol));
    }

    private static SelectStatement select(TableRef from, JoinClause... joins) {
        return new SelectStatement(true, List.of(), from, List.of(joins), null,
            List.of(), null, List.of(), null, null);
    }

    @Test
    void starSchema_reordersSmallestTableFirst() {
        // FROM a JOIN b ON a.id = b.aid JOIN c ON a.id = c.aid — both joins reference "a"
        // directly (star schema). b is huge, c is tiny: the cheaper join (c) should move first.
        cardinalities.put("s.a", 100L);
        cardinalities.put("s.b", 1_000_000L);
        cardinalities.put("s.c", 10L);

        TableRef a = table("s", "a", "a");
        JoinClause joinB = new JoinClause(table("s", "b", "b"), equi("a", "id", "b", "aid"));
        JoinClause joinC = new JoinClause(table("s", "c", "c"), equi("a", "id", "c", "aid"));

        SelectStatement stmt = select(a, joinB, joinC);
        List<JoinClause> ordered = optimizer().order(stmt);

        assertThat(ordered).containsExactly(joinC, joinB);
    }

    @Test
    void chainJoin_respectsDependencyOrder() {
        // FROM a JOIN b ON a.id = b.aid JOIN c ON b.id = c.bid — c's ON references b, not a
        // (a chain, not a star). b is huge and c is tiny, so a cost-only reordering would want
        // to run c before b — but QueryEngine executes joins strictly left-to-right, and c's
        // join key ("b.id") doesn't exist in the accumulated result until b has been joined.
        // Placing c first would make bind-join key extraction find nothing, silently collapsing
        // the whole query to zero rows. The optimizer must keep b before c here (or, since that
        // makes reordering pointless for a 2-join chain, simply fall back to the original order).
        cardinalities.put("s.a", 5L);
        cardinalities.put("s.b", 500_000L);
        cardinalities.put("s.c", 50L);

        TableRef a = table("s", "a", "a");
        JoinClause joinB = new JoinClause(table("s", "b", "b"), equi("a", "id", "b", "aid"));
        JoinClause joinC = new JoinClause(table("s", "c", "c"), equi("b", "id", "c", "bid"));

        SelectStatement stmt = select(a, joinB, joinC);
        List<JoinClause> ordered = optimizer().order(stmt);

        assertThat(ordered).containsExactly(joinB, joinC);
    }

    @Test
    void nonEquiJoin_fallsBackToOriginalOrder() {
        cardinalities.put("s.a", 100L);
        cardinalities.put("s.b", 1_000_000L);
        cardinalities.put("s.c", 10L);

        TableRef a = table("s", "a", "a");
        JoinClause joinB = new JoinClause(table("s", "b", "b"), equi("a", "id", "b", "aid"));
        // Range comparison, not an equi-join — the optimizer can't model this, must bail out.
        JoinClause joinC = new JoinClause(table("s", "c", "c"),
            new Condition.Compare(new Expr.Column("a", "id"), "<", new Expr.Column("c", "aid")));

        SelectStatement stmt = select(a, joinB, joinC);
        List<JoinClause> ordered = optimizer().order(stmt);

        assertThat(ordered).containsExactly(joinB, joinC);
    }

    @Test
    void fewerThanTwoJoins_returnsSameListUnchanged() {
        TableRef a = table("s", "a", "a");
        JoinClause joinB = new JoinClause(table("s", "b", "b"), equi("a", "id", "b", "aid"));
        SelectStatement stmt = select(a, joinB);

        List<JoinClause> ordered = optimizer().order(stmt);

        assertThat(ordered).containsExactly(joinB);
    }
}
