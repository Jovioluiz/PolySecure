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
