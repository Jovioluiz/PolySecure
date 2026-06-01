package com.polysecure.model;

import java.util.List;

public record InsertStatement(
    String tableName,
    List<StoreInsertClause> clauses
) implements Statement {}
