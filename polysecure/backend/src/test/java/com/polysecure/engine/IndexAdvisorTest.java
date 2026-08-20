/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.model.ColumnDefinition;
import com.polysecure.model.Condition;
import com.polysecure.model.CreateTableStatement;
import com.polysecure.model.DeleteStatement;
import com.polysecure.model.Expr;
import com.polysecure.model.JoinClause;
import com.polysecure.model.SelectStatement;
import com.polysecure.model.TableRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndexAdvisorTest {

    private static TableRef table(String store, String name, String alias) {
        return new TableRef(store, name, alias);
    }

    private static Expr.Column col(String alias, String name) {
        return new Expr.Column(alias, name);
    }

    private static SelectStatement selectWhere(TableRef from, Condition where, JoinClause... joins) {
        return new SelectStatement(true, List.of(), from, List.of(joins), where,
            List.of(), null, List.of(), null, null);
    }

    @Test
    void singleStoreSelect_accumulatesFrequencyAndSuggestsAboveThreshold() {
        IndexAdvisor advisor = new IndexAdvisor(new MetadataCatalog());
        TableRef orders = table("pg", "orders", "orders");
        Condition where = new Condition.Compare(col(null, "status"), "=", new Expr.Literal("enviado"));

        for (int i = 0; i < 8; i++) advisor.record(selectWhere(orders, where));

        List<IndexAdvisor.IndexSuggestion> suggestions = advisor.suggestions(5, 0.2);
        assertThat(suggestions).hasSize(1);
        IndexAdvisor.IndexSuggestion s = suggestions.get(0);
        assertThat(s.store()).isEqualTo("pg");
        assertThat(s.table()).isEqualTo("orders");
        assertThat(s.column()).isEqualTo("status");
        assertThat(s.occurrences()).isEqualTo(8);
        assertThat(s.observations()).isEqualTo(8);
        assertThat(s.frequency()).isEqualTo(1.0);
    }

    @Test
    void belowOccurrenceThreshold_isNotSuggested() {
        IndexAdvisor advisor = new IndexAdvisor(new MetadataCatalog());
        TableRef orders = table("pg", "orders", "orders");
        Condition where = new Condition.Compare(col(null, "status"), "=", new Expr.Literal("enviado"));

        for (int i = 0; i < 3; i++) advisor.record(selectWhere(orders, where));

        assertThat(advisor.suggestions(5, 0.2)).isEmpty();
        // But the raw counters still track it, for transparency below the threshold.
        assertThat(advisor.rawStats().get("predicateOccurrences"))
            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
            .containsEntry("pg.orders.status", 3L);
    }

    @Test
    void belowFrequencyThreshold_isNotSuggested() {
        IndexAdvisor advisor = new IndexAdvisor(new MetadataCatalog());
        TableRef orders = table("pg", "orders", "orders");
        Condition statusFilter = new Condition.Compare(col(null, "status"), "=", new Expr.Literal("enviado"));

        // status filtered in 5 of 30 total queries against the table -> ~16.7%, below 20%.
        for (int i = 0; i < 5; i++) advisor.record(selectWhere(orders, statusFilter));
        for (int i = 0; i < 25; i++) advisor.record(selectWhere(orders, null));

        assertThat(advisor.suggestions(5, 0.2)).isEmpty();
    }

    @Test
    void crossStoreJoin_resolvesQualifiedColumnToCorrectStoreTable() {
        IndexAdvisor advisor = new IndexAdvisor(new MetadataCatalog());
        TableRef pgOrders = table("pg", "orders", "o");
        TableRef mgOrders = table("mg", "orders", "m");
        JoinClause join = new JoinClause(mgOrders,
            new Condition.Compare(col("o", "id"), "=", col("m", "id")));
        Condition where = new Condition.Compare(col("m", "details"), "=", new Expr.Literal("x"));

        for (int i = 0; i < 6; i++) advisor.record(selectWhere(pgOrders, where, join));

        List<IndexAdvisor.IndexSuggestion> suggestions = advisor.suggestions(5, 0.2);
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).store()).isEqualTo("mg");
        assertThat(suggestions.get(0).table()).isEqualTo("orders");
        assertThat(suggestions.get(0).column()).isEqualTo("details");
    }

    @Test
    void unqualifiedColumnAcrossJoin_isSkippedRatherThanGuessed() {
        IndexAdvisor advisor = new IndexAdvisor(new MetadataCatalog());
        TableRef pgOrders = table("pg", "orders", "o");
        TableRef mgOrders = table("mg", "orders", "m");
        JoinClause join = new JoinClause(mgOrders,
            new Condition.Compare(col("o", "id"), "=", col("m", "id")));
        // No table alias, and two tables are in scope — ambiguous, must not be attributed.
        Condition where = new Condition.Compare(col(null, "total"), ">", new Expr.Literal(100L));

        for (int i = 0; i < 10; i++) advisor.record(selectWhere(pgOrders, where, join));

        assertThat(advisor.suggestions(1, 0.0)).isEmpty();
    }

    @Test
    void updateStatement_resolvesWhereColumnViaMetadataCatalog() {
        MetadataCatalog catalog = new MetadataCatalog();
        catalog.register(new CreateTableStatement("orders", List.of(
            new ColumnDefinition("id", "INT", "pg", true),
            new ColumnDefinition("details", "DOCUMENT", "mg", false)
        )));
        IndexAdvisor advisor = new IndexAdvisor(catalog);

        Condition where = new Condition.Compare(col(null, "details"), "=", new Expr.Literal("y"));
        for (int i = 0; i < 5; i++) {
            advisor.record(new com.polysecure.model.UpdateStatement("orders", List.of(), where));
        }

        List<IndexAdvisor.IndexSuggestion> suggestions = advisor.suggestions(5, 0.2);
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).store()).isEqualTo("mg");
        assertThat(suggestions.get(0).table()).isEqualTo("orders");
        assertThat(suggestions.get(0).column()).isEqualTo("details");
    }

    @Test
    void deleteStatement_unknownTable_isIgnoredWithoutThrowing() {
        IndexAdvisor advisor = new IndexAdvisor(new MetadataCatalog());
        Condition where = new Condition.Compare(col(null, "id"), "=", new Expr.Literal(1L));
        advisor.record(new DeleteStatement("does_not_exist", where));
        assertThat(advisor.suggestions()).isEmpty();
    }
}
