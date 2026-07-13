/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.engine;

import java.util.Map;

public record DmlResult(
    String operation,
    String table,
    Map<String, Integer> affectedByStore,
    String message
) {}
