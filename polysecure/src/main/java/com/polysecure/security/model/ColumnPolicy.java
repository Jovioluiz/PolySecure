package com.polysecure.security.model;

import java.util.Set;

public record ColumnPolicy(Set<String> excludedColumns) {}
