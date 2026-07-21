/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.adapter.StoreAdapter;
import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.catalog.StoreRegistry;
import com.polysecure.model.ColumnDefinition;
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

class SemanticValidatorColumnTest {

    private SemanticValidator validator;
    private StoreAdapter adapter;

    @BeforeEach
    void setUp() {
        StoreRegistry registry = mock(StoreRegistry.class);
        adapter = mock(StoreAdapter.class);
        when(registry.exists(anyString())).thenReturn(true);
        when(registry.get("mss")).thenReturn(adapter);
        when(adapter.getSchema("produtos")).thenReturn(List.of(
            new ColumnDefinition("id", "INT", "mss", true),
            new ColumnDefinition("categoria", "TEXT", "mss", false),
            new ColumnDefinition("QuantidadeEstoque", "INT", "mss", false)
        ));
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
    void aggregateOnUnknownColumnIsRejected() {
        SelectStatement stmt = new SelectStatement(
            false,
            List.of(agg("AVG", null, "estoque", "media"), plain(null, "categoria")),
            new TableRef("mss", "produtos", null),
            List.of(),
            null,
            List.of(new Expr.Column(null, "categoria")),
            null,
            List.of(),
            null,
            null
        );

        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(stmt));
        assert ex.getMessage().contains("estoque");
        assert ex.getMessage().contains("QuantidadeEstoque");
    }

    @Test
    void aggregateOnRealColumnIsAccepted() {
        SelectStatement stmt = new SelectStatement(
            false,
            List.of(agg("AVG", null, "QuantidadeEstoque", "media"), plain(null, "categoria")),
            new TableRef("mss", "produtos", null),
            List.of(),
            null,
            List.of(new Expr.Column(null, "categoria")),
            null,
            List.of(),
            null,
            null
        );

        assertDoesNotThrow(() -> validator.validate(stmt));
    }

    @Test
    void unknownQualifiedColumnInWhereIsRejected() {
        SelectStatement stmt = new SelectStatement(
            true,
            List.of(),
            new TableRef("mss", "produtos", "p"),
            List.of(),
            new com.polysecure.model.Condition.Compare(
                new Expr.Column("p", "preco"), "=", new Expr.Literal(10)),
            List.of(),
            null,
            List.of(),
            null,
            null
        );

        SemanticException ex = assertThrows(SemanticException.class, () -> validator.validate(stmt));
        assert ex.getMessage().contains("p.preco");
    }
}
