package com.polysecure.model;

import java.util.List;

public record InsertSelectStatement(
    String tableName,
    List<StoreTargetClause> targets,
    SelectStatement source
) implements Statement {}
