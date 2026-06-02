package com.polysecure.model;

import java.util.List;

public record StoreInsertClause(String storeName, List<String> columns, List<Object> values) {}
