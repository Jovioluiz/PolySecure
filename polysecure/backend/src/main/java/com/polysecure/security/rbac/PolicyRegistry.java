/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.security.rbac;

import com.polysecure.security.model.AnonymizationMethod;
import com.polysecure.security.model.AnonymizationPolicy;
import com.polysecure.security.model.AnonymizationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PolicyRegistry {

    private final ConcurrentHashMap<String, AnonymizationPolicy> policies = new ConcurrentHashMap<>();

    public PolicyRegistry() {
        // LGPD compliance policy — matches column names by regex pattern
        policies.put("lgpd_compliance", new AnonymizationPolicy("lgpd_compliance", List.of(
            new AnonymizationRule("cpf|rg|cnpj",       AnonymizationMethod.HASH_SHA256),
            new AnonymizationRule("email",              AnonymizationMethod.TRUNCATE_DOMAIN),
            new AnonymizationRule("telefone|phone|cel", AnonymizationMethod.MASK_LAST_DIGITS),
            new AnonymizationRule("nome|name",          AnonymizationMethod.PSEUDONYMIZE)
        )));
    }

    public Optional<AnonymizationPolicy> getPolicy(String name) {
        return Optional.ofNullable(policies.get(name));
    }

    public void register(AnonymizationPolicy policy) {
        policies.put(policy.name(), policy);
    }
}
