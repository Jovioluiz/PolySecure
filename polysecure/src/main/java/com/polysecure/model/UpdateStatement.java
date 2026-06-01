package com.polysecure.model;

import java.util.List;

public record UpdateStatement(
    String tableName,
    List<SetClause> sets,
    Condition where
) implements Statement {}
