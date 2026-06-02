package com.polysecure.model;

import java.util.List;

public sealed interface Condition permits Condition.And, Condition.Or, Condition.Compare, Condition.In {

    record And(Condition left, Condition right) implements Condition {}

    record Or(Condition left, Condition right) implements Condition {}

    record Compare(Expr left, String op, Expr right) implements Condition {}

    // Used by bind-join: col IN (v1, v2, ...) pushed down to the inner store
    record In(Expr expr, List<Object> values) implements Condition {}
}
