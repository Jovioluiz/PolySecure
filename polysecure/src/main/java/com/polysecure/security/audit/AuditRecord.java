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
