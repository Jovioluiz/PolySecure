package com.polysecure.engine;

import com.polysecure.model.Condition;
import com.polysecure.model.Expr;

import java.util.Map;

class ConditionEvaluator {

    boolean evaluate(Condition cond, Map<String, Object> row) {
        return switch (cond) {
            case Condition.And a -> evaluate(a.left(), row) && evaluate(a.right(), row);
            case Condition.Or  o -> evaluate(o.left(), row) || evaluate(o.right(), row);
            case Condition.Compare c -> compare(resolve(c.left(), row), c.op(), resolve(c.right(), row));
            case Condition.In i -> {
                Object val = resolve(i.expr(), row);
                yield val != null && i.values().stream().anyMatch(v -> compare(val, "=", v));
            }
        };
    }

    private Object resolve(Expr e, Map<String, Object> row) {
        return switch (e) {
            case Expr.Literal lit -> lit.value();
            case Expr.Column col -> {
                // Look up "alias.column" first, then just "column"
                String qualified = col.tableAlias() != null ? col.tableAlias() + "." + col.name() : null;
                if (qualified != null && row.containsKey(qualified)) yield row.get(qualified);
                yield row.get(col.name());
            }
            case Expr.Star s -> null;
        };
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
        // Numeric comparison
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
        // Comparable (String, etc.)
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
