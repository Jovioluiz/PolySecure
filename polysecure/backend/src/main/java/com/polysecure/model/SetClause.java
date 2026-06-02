package com.polysecure.model;

// Represents one assignment in UPDATE POLYSTORE ... SET store.col = value
public record SetClause(
    String store,   // null = apply to all stores that have this column
    String column,
    Object value
) {}
