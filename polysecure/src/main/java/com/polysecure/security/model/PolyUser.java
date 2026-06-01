package com.polysecure.security.model;

public record PolyUser(String username, String passwordHash, String roleName) {}
