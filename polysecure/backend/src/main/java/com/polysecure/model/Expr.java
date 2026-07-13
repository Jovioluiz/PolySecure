/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

public sealed interface Expr permits Expr.Column, Expr.Literal, Expr.Star, Expr.Aggregate {

    record Column(String tableAlias, String name) implements Expr {}

    record Literal(Object value) implements Expr {}

    // Represents alias.* in SELECT
    record Star(String tableAlias) implements Expr {}

    // Represents aggregate function: COUNT(*), SUM(col), AVG(col), MIN(col), MAX(col)
    record Aggregate(String func, Expr arg) implements Expr {}
}
