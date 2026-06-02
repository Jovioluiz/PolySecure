package com.polysecure.security.model;

public enum AnonymizationMethod {
    HASH_SHA256,
    TRUNCATE_DOMAIN,
    MASK_LAST_DIGITS,
    PSEUDONYMIZE,
    REDACT
}
