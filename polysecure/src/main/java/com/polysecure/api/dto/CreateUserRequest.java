package com.polysecure.api.dto;

public record CreateUserRequest(String username, String password, String roleName) {}
