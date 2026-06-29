package com.polysecure.adapter;

public record StoreCapabilities(
    boolean supportsSelect,
    boolean supportsInsert,
    boolean supportsUpdate,
    boolean supportsDelete,
    boolean supportsDdl,
    boolean supportsTransactions
) {
    public static StoreCapabilities full() {
        return new StoreCapabilities(true, true, true, true, true, true);
    }

    public static StoreCapabilities readOnly() {
        return new StoreCapabilities(true, false, false, false, false, false);
    }
}
