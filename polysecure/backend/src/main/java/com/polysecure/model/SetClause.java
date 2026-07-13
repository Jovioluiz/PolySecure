/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

// Represents one assignment in UPDATE POLYSTORE ... SET store.col = value
public record SetClause(
    String store,   // null = apply to all stores that have this column
    String column,
    Object value
) {}
