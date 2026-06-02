package com.polysecure.api.dto;

import com.polysecure.catalog.StoreConfig.StoreType;

public record RegisterStoreRequest(
    String name,
    StoreType type,     // POSTGRES, MONGODB, NEO4J
    String host,
    int port,
    String database,
    String username,
    String password
) {}
