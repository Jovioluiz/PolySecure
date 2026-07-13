/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.security.anonymization;

import com.polysecure.security.model.AnonymizationPolicy;
import com.polysecure.security.model.AnonymizationRule;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AnonymizationEngine {

    public List<Map<String, Object>> apply(List<Map<String, Object>> rows, AnonymizationPolicy policy) {
        List<CompiledRule> compiled = policy.rules().stream()
            .map(r -> new CompiledRule(Pattern.compile(r.fieldPattern(), Pattern.CASE_INSENSITIVE), r))
            .collect(Collectors.toList());
        return rows.stream().map(row -> applyToRow(row, compiled)).collect(Collectors.toList());
    }

    public List<String> affectedColumns(Map<String, Object> sampleRow, AnonymizationPolicy policy) {
        List<String> affected = new ArrayList<>();
        for (AnonymizationRule rule : policy.rules()) {
            Pattern p = Pattern.compile(rule.fieldPattern(), Pattern.CASE_INSENSITIVE);
            sampleRow.keySet().stream().filter(k -> p.matcher(k).find()).forEach(affected::add);
        }
        return affected.stream().distinct().collect(Collectors.toList());
    }

    private Map<String, Object> applyToRow(Map<String, Object> row, List<CompiledRule> rules) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        for (CompiledRule cr : rules) {
            result.replaceAll((col, val) -> {
                if (cr.pattern().matcher(col).find() && val instanceof String s) {
                    return switch (cr.rule().method()) {
                        case HASH_SHA256      -> Anonymizers.hashSha256(s);
                        case TRUNCATE_DOMAIN  -> Anonymizers.truncateDomain(s);
                        case MASK_LAST_DIGITS -> Anonymizers.maskLastDigits(s);
                        case PSEUDONYMIZE     -> Anonymizers.pseudonymize(s);
                        case REDACT           -> Anonymizers.redact(s);
                    };
                }
                return val;
            });
        }
        return result;
    }

    private record CompiledRule(Pattern pattern, AnonymizationRule rule) {}
}
