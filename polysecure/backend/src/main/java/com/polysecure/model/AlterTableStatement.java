/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

public record AlterTableStatement(
    String tableName,
    AlterOp operation,
    ColumnDefinition addColumn,
    String dropColumnName,
    String dropStoreName
) implements Statement {
    public enum AlterOp { ADD_COLUMN, DROP_COLUMN }
}
