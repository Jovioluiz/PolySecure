package com.polysecure.model;

public sealed interface Expr permits Expr.Column, Expr.Literal, Expr.Star {

    record Column(String tableAlias, String name) implements Expr {}

    record Literal(Object value) implements Expr {}

    // Represents alias.* in SELECT
    record Star(String tableAlias) implements Expr {}
}
