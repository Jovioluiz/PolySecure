/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.security.audit;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
public class AuditLogger {

    private static final int MAX_RECORDS = 10_000;

    private final Deque<AuditRecord> log = new ArrayDeque<>();

    public synchronized void log(AuditRecord record) {
        if (log.size() >= MAX_RECORDS) log.pollFirst();
        log.addLast(record);
    }

    public synchronized List<AuditRecord> getAll() {
        return List.copyOf(log);
    }

    public synchronized List<AuditRecord> getLastN(int n) {
        return log.stream().skip(Math.max(0, log.size() - n)).toList();
    }
}
