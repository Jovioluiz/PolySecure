package com.polysecure.api.dto;

import com.polysecure.catalog.StoreConfig.StoreType;

public record RegisterStoreRequest(
    String name,
    StoreType type,
    String host,
    int port,
    String database,
    String username,
    String password
) {}
