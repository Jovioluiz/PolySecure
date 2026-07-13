/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

public record ColumnDefinition(
    String name,
    String type,        // INT, TEXT, BOOLEAN, DOCUMENT, GRAPH, etc.
    String store,       // target store name
    boolean primaryKey
) {}
