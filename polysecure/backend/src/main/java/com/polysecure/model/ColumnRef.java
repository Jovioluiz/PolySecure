/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

public record ColumnRef(String tableAlias, String column, String outputAlias, Expr aggExpr) {

    /** Backward-compatible constructor for non-aggregate projections. */
    public ColumnRef(String tableAlias, String column, String outputAlias) {
        this(tableAlias, column, outputAlias, null);
    }

    public String effectiveOutputName() {
        return outputAlias != null ? outputAlias : column;
    }

    public boolean isStar() {
        return "*".equals(column);
    }

    public boolean isAggregate() {
        return aggExpr instanceof Expr.Aggregate;
    }
}
