/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.security.audit;

import java.time.Instant;
import java.util.List;

public record AuditRecord(
    Instant timestamp,
    String username,
    String roleName,
    String query,
    List<String> storesAccessed,
    int rowsReturned,
    List<String> anonymizationApplied,
    long durationMs
) {}
