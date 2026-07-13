/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

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
