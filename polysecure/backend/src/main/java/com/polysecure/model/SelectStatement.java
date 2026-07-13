/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

import java.util.List;

public record SelectStatement(
    boolean star,
    List<ColumnRef> projections,
    TableRef from,
    List<JoinClause> joins,
    Condition where,
    List<Expr> groupBy,
    Condition having,
    List<OrderItem> orderBy,
    Integer limit,
    Integer offset
) implements Statement {}
