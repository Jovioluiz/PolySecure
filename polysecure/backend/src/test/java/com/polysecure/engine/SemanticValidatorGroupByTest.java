/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.model.ColumnRef;
import com.polysecure.model.Expr;
import com.polysecure.model.SelectStatement;
import com.polysecure.model.TableRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SemanticValidatorGroupByTest {

    private SemanticValidator validator;

    @BeforeEach
    void setUp() {
        StoreRegistry registry = mock(StoreRegistry.class);
        when(registry.exists(anyString())).thenReturn(true);
        validator = new SemanticValidator(registry, new MetadataCatalog());
    }

    private static ColumnRef plain(String alias, String column) {
        return new ColumnRef(alias, column, null);
    }

    private static ColumnRef agg(String func, String alias, String column, String outputAlias) {
        Expr.Aggregate aggExpr = new Expr.Aggregate(func, new Expr.Column(alias, column));
        return new ColumnRef(alias, column, outputAlias, aggExpr);
    }

    @Test
    void mixingAggregateAndPlainColumnsWithoutGroupByIsRejected() {
        SelectStatement stmt = new SelectStatement(
            false,
            List.of(plain("p", "nome"), agg("COUNT", "p", "*", "total")),
            new TableRef("pg", "produtos", "p"),
            List.of(),
            null,
            List.of(),
            null,
            List.of(),
            null,
            null
        );

        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(stmt));
        assert ex.getMessage().contains("GROUP BY");
    }

    @Test
    void plainColumnMissingFromGroupByIsRejected() {
        SelectStatement stmt = new SelectStatement(
            false,
            List.of(plain("p", "nome"), plain("p", "categoria"), agg("SUM", "p", "preco", "total")),
            new TableRef("pg", "produtos", "p"),
            List.of(),
            null,
            List.of(new Expr.Column("p", "nome")),
            null,
            List.of(),
            null,
            null
        );

        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(stmt));
        assert ex.getMessage().contains("categoria");
    }

    @Test
    void allPlainColumnsCoveredByGroupByIsAccepted() {
        SelectStatement stmt = new SelectStatement(
            false,
            List.of(plain("p", "nome"), agg("SUM", "p", "preco", "total")),
            new TableRef("pg", "produtos", "p"),
            List.of(),
            null,
            List.of(new Expr.Column("p", "nome")),
            null,
            List.of(),
            null,
            null
        );

        assertDoesNotThrow(() -> validator.validate(stmt));
    }

    @Test
    void bareAggregateWithNoPlainColumnsNeedsNoGroupBy() {
        SelectStatement stmt = new SelectStatement(
            false,
            List.of(agg("COUNT", "p", "*", "total")),
            new TableRef("pg", "produtos", "p"),
            List.of(),
            null,
            List.of(),
            null,
            List.of(),
            null,
            null
        );

        assertDoesNotThrow(() -> validator.validate(stmt));
    }
}
