package com.polysecure.model;

public record ColumnDefinition(
    String name,
    String type,        // INT, TEXT, BOOLEAN, DOCUMENT, GRAPH, etc.
    String store,       // target store name
    boolean primaryKey
) {}
