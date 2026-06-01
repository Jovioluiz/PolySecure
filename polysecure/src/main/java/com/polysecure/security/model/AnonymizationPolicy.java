package com.polysecure.security.model;

import java.util.List;

public record AnonymizationPolicy(String name, List<AnonymizationRule> rules) {}
