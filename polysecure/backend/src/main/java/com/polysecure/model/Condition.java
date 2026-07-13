/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

import java.util.List;

public sealed interface Condition
        permits Condition.And, Condition.Or, Condition.Compare, Condition.In,
                Condition.Not, Condition.IsNull, Condition.Like, Condition.Between {

    record And(Condition left, Condition right) implements Condition {}

    record Or(Condition left, Condition right) implements Condition {}

    record Compare(Expr left, String op, Expr right) implements Condition {}

    // Membership: col IN (v1, v2, ...) — also used internally by bind-join
    record In(Expr expr, List<Object> values) implements Condition {}

    // Negation: NOT (inner)
    record Not(Condition inner) implements Condition {}

    // Null check: col IS NULL
    record IsNull(Expr expr) implements Condition {}

    // Pattern match: col LIKE 'pattern%'
    record Like(Expr expr, String pattern) implements Condition {}

    // Range: col BETWEEN low AND high
    record Between(Expr expr, Object low, Object high) implements Condition {}
}
