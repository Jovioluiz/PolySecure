package com.polysecure.model;

import java.util.List;

public record CreateTableStatement(
    String tableName,
    List<ColumnDefinition> columns
) implements Statement {}
