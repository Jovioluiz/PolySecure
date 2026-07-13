/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.security.rbac;

import com.polysecure.security.model.PolyUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserRegistry {

    private final ConcurrentHashMap<String, PolyUser> users = new ConcurrentHashMap<>();
    private final PasswordEncoder encoder;

    public UserRegistry(PasswordEncoder encoder,
                        @Value("${polysecure.admin.username:admin}") String adminUsername,
                        @Value("${polysecure.admin.password:admin123}") String adminPassword) {
        this.encoder = encoder;
        users.put(adminUsername,
            new PolyUser(adminUsername, encoder.encode(adminPassword), "dba_admin"));
    }

    public void register(String username, String rawPassword, String roleName) {
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("User already exists: '" + username + "'");
        }
        users.put(username, new PolyUser(username, encoder.encode(rawPassword), roleName));
    }

    public Optional<PolyUser> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public boolean verifyPassword(String username, String rawPassword) {
        PolyUser user = users.get(username);
        return user != null && encoder.matches(rawPassword, user.passwordHash());
    }

    public Collection<PolyUser> listAll() {
        return users.values();
    }
}
