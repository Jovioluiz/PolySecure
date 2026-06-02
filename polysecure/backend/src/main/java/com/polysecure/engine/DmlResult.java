package com.polysecure.engine;

import java.util.Map;

public record DmlResult(
    String operation,
    String table,
    Map<String, Integer> affectedByStore,
    String message
) {}
