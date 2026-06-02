package com.polysecure.api.dto;

import java.util.List;
import java.util.Map;

public record QueryResponse(int count, List<Map<String, Object>> rows) {}
