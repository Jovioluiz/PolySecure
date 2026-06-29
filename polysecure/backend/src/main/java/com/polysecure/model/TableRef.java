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
