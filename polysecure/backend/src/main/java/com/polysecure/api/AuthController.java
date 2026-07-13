/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.api;

import com.polysecure.api.dto.LoginRequest;
import com.polysecure.api.dto.LoginResponse;
import com.polysecure.security.auth.JwtService;
import com.polysecure.security.model.PolyUser;
import com.polysecure.security.rbac.UserRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRegistry userRegistry;
    private final JwtService jwtService;

    public AuthController(UserRegistry userRegistry, JwtService jwtService) {
        this.userRegistry = userRegistry;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (!userRegistry.verifyPassword(request.username(), request.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid credentials\"}");
        }
        PolyUser user = userRegistry.findByUsername(request.username()).orElseThrow();
        String token = jwtService.generateToken(user.username());
        return ResponseEntity.ok(new LoginResponse(token, user.username(), user.roleName()));
    }
}
