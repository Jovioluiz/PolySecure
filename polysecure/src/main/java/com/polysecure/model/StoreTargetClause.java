package com.polysecure.model;

import java.util.List;

public record StoreTargetClause(String storeName, List<String> columns) {}
