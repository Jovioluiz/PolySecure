package com.polysecure.model;

import java.util.List;

public record SelectStatement(
    boolean star,
    List<ColumnRef> projections,
    TableRef from,
    List<JoinClause> joins,
    Condition where
) implements Statement {}
