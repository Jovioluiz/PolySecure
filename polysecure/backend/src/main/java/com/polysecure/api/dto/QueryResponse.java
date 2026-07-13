/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.api.dto;

import java.util.List;
import java.util.Map;

public record QueryResponse(int count, List<Map<String, Object>> rows) {}
