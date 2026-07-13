/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.adapter.jdbc;

import com.polysecure.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds parameterized SQL snippets from a LocalSelectQuery or a Condition tree.
 * Shared by all JDBC-based adapters.
 */
public final class SqlBuilder {

    private final List<Object> params = new ArrayList<>();

    public void addParam(Object v) { params.add(v); }
    public List<Object> params() { return params; }

    public String build(LocalSelectQuery query) {
        StringBuilder sb = new StringBuilder("SELECT ");
        if (query.star() || query.projections().isEmpty()) {
            sb.append("*");
        } else {
            sb.append(query.projections().stream()
                .map(c -> c.tableAlias() != null ? c.tableAlias() + "." + c.column() : c.column())
                .collect(Collectors.joining(", ")));
        }
        sb.append(" FROM ").append(query.table());
        if (!query.table().equals(query.alias())) sb.append(" ").append(query.alias());
        if (query.where() != null) {
            String w = renderCondition(query.where(), query.alias());
            if (!w.isBlank()) sb.append(" WHERE ").append(w);
        }
        return sb.toString();
    }

    public String renderCondition(Condition cond, String alias) {
        return switch (cond) {
            case Condition.And a -> {
                String l = renderCondition(a.left(), alias);
                String r = renderCondition(a.right(), alias);
                yield l.isBlank() || r.isBlank() ? l + r : "(" + l + " AND " + r + ")";
            }
            case Condition.Or o -> {
                String l = renderCondition(o.left(), alias);
                String r = renderCondition(o.right(), alias);
                yield l.isBlank() || r.isBlank() ? "" : "(" + l + " OR " + r + ")";
            }
            case Condition.Not n -> {
                String inner = renderCondition(n.inner(), alias);
                yield inner.isBlank() ? "" : "NOT (" + inner + ")";
            }
            case Condition.Compare c  -> renderCompare(c, alias);
            case Condition.In i       -> renderIn(i);
            case Condition.IsNull isn -> renderExpr(isn.expr()) + " IS NULL";
            case Condition.Like like  -> {
                params.add(like.pattern());
                yield renderExpr(like.expr()) + " LIKE ?";
            }
            case Condition.Between between -> {
                String field = renderExpr(between.expr());
                params.add(between.low());
                params.add(between.high());
                yield field + " BETWEEN ? AND ?";
            }
        };
    }

    private String renderIn(Condition.In i) {
        if (i.values().isEmpty()) return "1=0";
        String field = renderExpr(i.expr());
        String placeholders = i.values().stream()
            .map(v -> { params.add(v); return "?"; })
            .collect(Collectors.joining(", "));
        return field + " IN (" + placeholders + ")";
    }

    private String renderCompare(Condition.Compare c, String alias) {
        if (alias != null && referencesOtherAlias(c, alias)) return "";
        return renderExpr(c.left()) + " " + c.op() + " " + renderExpr(c.right());
    }

    public String renderExpr(Expr e) {
        return switch (e) {
            case Expr.Column col -> col.tableAlias() != null
                ? col.tableAlias() + "." + col.name() : col.name();
            case Expr.Literal lit -> { params.add(lit.value()); yield "?"; }
            case Expr.Star s -> s.tableAlias() != null ? s.tableAlias() + ".*" : "*";
            case Expr.Aggregate agg -> agg.func().toUpperCase() + "("
                + (agg.arg() instanceof Expr.Star ? "*" : renderExpr(agg.arg())) + ")";
        };
    }

    private boolean referencesOtherAlias(Condition.Compare c, String alias) {
        return isOther(c.left(), alias) || isOther(c.right(), alias);
    }

    private boolean isOther(Expr e, String alias) {
        return e instanceof Expr.Column col
            && col.tableAlias() != null
            && !col.tableAlias().equals(alias);
    }
}
