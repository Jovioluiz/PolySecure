/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

import java.util.List;

// Single-store sub-query sent to a StoreAdapter
public record LocalSelectQuery(
    String storeName,
    String table,
    String alias,
    boolean star,
    List<ColumnRef> projections,
    Condition where
) {}
