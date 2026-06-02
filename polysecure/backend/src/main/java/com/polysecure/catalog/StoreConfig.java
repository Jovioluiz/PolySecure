package com.polysecure.catalog;

public record StoreConfig(
    String name,
    StoreType type,
    String host,
    int port,
    String database,
    String username,
    String password
) {
    public enum StoreType { POSTGRES, MONGODB, NEO4J }
}
