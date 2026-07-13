/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.model.Condition;
import com.polysecure.model.Expr;

import java.util.Map;

class ConditionEvaluator {

    boolean evaluate(Condition cond, Map<String, Object> row) {
        return switch (cond) {
            case Condition.And a     -> evaluate(a.left(), row) && evaluate(a.right(), row);
            case Condition.Or  o     -> evaluate(o.left(), row) || evaluate(o.right(), row);
            case Condition.Not n     -> !evaluate(n.inner(), row);
            case Condition.Compare c -> compare(resolve(c.left(), row), c.op(), resolve(c.right(), row));
            case Condition.In i -> {
                Object val = resolve(i.expr(), row);
                yield val != null && i.values().stream().anyMatch(v -> compare(val, "=", v));
            }
            case Condition.IsNull isnull -> resolve(isnull.expr(), row) == null;
            case Condition.Like like -> {
                Object val = resolve(like.expr(), row);
                yield val != null && matchLike(val.toString(), like.pattern());
            }
            case Condition.Between between -> {
                Object val = resolve(between.expr(), row);
                yield val != null
                    && compare(val, ">=", between.low())
                    && compare(val, "<=", between.high());
            }
        };
    }

    private Object resolve(Expr e, Map<String, Object> row) {
        return switch (e) {
            case Expr.Literal lit -> lit.value();
            case Expr.Column col -> {
                String qualified = col.tableAlias() != null ? col.tableAlias() + "." + col.name() : null;
                if (qualified != null && row.containsKey(qualified)) yield row.get(qualified);
                if (row.containsKey(col.name())) yield row.get(col.name());
                // Fallback: row keys are prefixed (alias.col) — search by suffix
                String suffix = "." + col.name();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getKey().endsWith(suffix)) yield entry.getValue();
                }
                yield null;
            }
            case Expr.Star s -> null;
            // Aggregates in HAVING are stored by their expression string (e.g. "COUNT(*)")
            case Expr.Aggregate agg -> row.getOrDefault(aggToKey(agg), null);
        };
    }

    static String aggToKey(Expr.Aggregate agg) {
        String argStr = switch (agg.arg()) {
            case Expr.Star ignored -> "*";
            case Expr.Column col -> col.tableAlias() != null
                ? col.tableAlias() + "." + col.name() : col.name();
            case Expr.Literal lit -> String.valueOf(lit.value());
            case Expr.Aggregate nested -> aggToKey(nested);
        };
        return agg.func().toUpperCase() + "(" + argStr + ")";
    }

    // Converts SQL LIKE pattern (% = any, _ = one char) to Java regex match
    private boolean matchLike(String value, String pattern) {
        StringBuilder regex = new StringBuilder("(?s)");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '%') {
                regex.append(".*");
            } else if (c == '_') {
                regex.append(".");
            } else {
                regex.append(java.util.regex.Pattern.quote(String.valueOf(c)));
            }
        }
        return value.matches(regex.toString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean compare(Object left, String op, Object right) {
        if (left == null || right == null) {
            return switch (op) {
                case "=" -> left == right;
                case "!=", "<>" -> left != right;
                default -> false;
            };
        }
        // Coerce String↔Number for cross-store comparisons (e.g. Solr returns "1", JDBC returns 1)
        if (left instanceof String sl && right instanceof Number) {
            try { left = Double.parseDouble(sl); } catch (NumberFormatException ignored) {}
        } else if (right instanceof String sr && left instanceof Number) {
            try { right = Double.parseDouble(sr); } catch (NumberFormatException ignored) {}
        }
        if (left instanceof Number l && right instanceof Number r) {
            double ld = l.doubleValue(), rd = r.doubleValue();
            return switch (op) {
                case "="  -> ld == rd;
                case "!=", "<>" -> ld != rd;
                case ">"  -> ld > rd;
                case "<"  -> ld < rd;
                case ">=" -> ld >= rd;
                case "<=" -> ld <= rd;
                default -> false;
            };
        }
        if (left instanceof Comparable && left.getClass().equals(right.getClass())) {
            int cmp = ((Comparable) left).compareTo(right);
            return switch (op) {
                case "="  -> cmp == 0;
                case "!=", "<>" -> cmp != 0;
                case ">"  -> cmp > 0;
                case "<"  -> cmp < 0;
                case ">=" -> cmp >= 0;
                case "<=" -> cmp <= 0;
                default -> false;
            };
        }
        return switch (op) {
            case "="  -> left.equals(right);
            case "!=", "<>" -> !left.equals(right);
            default -> false;
        };
    }
}
