package com.polysecure.model;

public record DeleteStatement(
    String tableName,
    Condition where
) implements Statement {}
