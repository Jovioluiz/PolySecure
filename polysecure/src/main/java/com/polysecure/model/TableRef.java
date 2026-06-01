package com.polysecure.model;

public record TableRef(String store, String table, String alias) {

    public String effectiveAlias() {
        return alias != null ? alias : table;
    }

    public boolean isCrossStore() {
        return store != null && !store.isBlank();
    }
}
