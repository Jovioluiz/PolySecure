package com.polysecure.model;

public record DropTableStatement(String tableName) implements Statement {}
