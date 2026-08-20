/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import com.polysecure.model.Condition;
import com.polysecure.model.Expr;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    private static Expr.Column col(String name) {
        return new Expr.Column(null, name);
    }

    private static Expr.Literal lit(Object v) {
        return new Expr.Literal(v);
    }

    @Test
    void evaluatesArithmeticInCompareCondition() {
        // WHERE (price * qty) > 100, row: price=25, qty=5 -> 125 > 100
        Condition cond = new Condition.Compare(
            new Expr.Arithmetic(col("price"), "*", col("qty")), ">", lit(100L));
        Map<String, Object> row = Map.of("price", 25L, "qty", 5L);
        assertThat(evaluator.evaluate(cond, row)).isTrue();
    }

    @Test
    void arithmeticRespectsOperatorPrecedenceWhenEvaluated() {
        // price + tax * 2, row: price=10, tax=3 -> 10 + (3*2) = 16, not (10+3)*2 = 26
        Expr expr = new Expr.Arithmetic(col("price"), "+", new Expr.Arithmetic(col("tax"), "*", lit(2L)));
        Condition cond = new Condition.Compare(expr, "=", lit(16L));
        Map<String, Object> row = Map.of("price", 10L, "tax", 3L);
        assertThat(evaluator.evaluate(cond, row)).isTrue();
    }

    @Test
    void divisionByZeroYieldsNullNotException() {
        Condition cond = new Condition.Compare(
            new Expr.Arithmetic(col("a"), "/", col("zero")), ">", lit(0L));
        Map<String, Object> row = Map.of("a", 10L, "zero", 0L);
        // null propagates through compare() as "no match" for a non-equality op, never throws.
        assertThat(evaluator.evaluate(cond, row)).isFalse();
    }

    @Test
    void coercesNumericStringsInArithmetic() {
        // Some stores (e.g. Solr) return numeric fields as strings.
        Condition cond = new Condition.Compare(
            new Expr.Arithmetic(col("price"), "-", col("discount")), "=", lit(80L));
        Map<String, Object> row = Map.of("price", "100", "discount", "20");
        assertThat(evaluator.evaluate(cond, row)).isTrue();
    }

    @Test
    void missingColumnPropagatesNullThroughArithmetic() {
        Condition cond = new Condition.Compare(
            new Expr.Arithmetic(col("price"), "+", col("missing")), "=", lit(999L));
        Map<String, Object> row = Map.of("price", 10L);
        assertThat(evaluator.evaluate(cond, row)).isFalse();
    }
}
