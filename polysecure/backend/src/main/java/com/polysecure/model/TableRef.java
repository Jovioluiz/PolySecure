/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.model;

public record TableRef(String store, String table, String alias) {

    public String effectiveAlias() {
        if (alias != null) return alias;
        if (store != null && !store.isBlank()) return store;
        return table;
    }

    public boolean isCrossStore() {
        return store != null && !store.isBlank();
    }
}
