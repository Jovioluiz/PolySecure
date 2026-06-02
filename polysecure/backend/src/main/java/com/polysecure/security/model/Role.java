package com.polysecure.security.model;

import java.util.Map;
import java.util.Set;

/**
 * storePermissions: store name → set of allowed operations. Use "*" to match all stores.
 * tablePolicies: "store.table" key → columns excluded from SELECT results.
 * anonymizationPolicyName: policy applied to SELECT results; null means no anonymization.
 */
public record Role(
    String name,
    Map<String, Set<Permission>> storePermissions,
    Map<String, ColumnPolicy> tablePolicies,
    String anonymizationPolicyName
) {}
