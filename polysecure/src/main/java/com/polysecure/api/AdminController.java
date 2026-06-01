package com.polysecure.api;

import com.polysecure.api.dto.CreateUserRequest;
import com.polysecure.engine.MaterializedViewCache;
import com.polysecure.engine.QueryEngine;
import com.polysecure.security.audit.AuditLogger;
import com.polysecure.security.audit.AuditRecord;
import com.polysecure.security.rbac.RoleRegistry;
import com.polysecure.security.rbac.UserRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserRegistry userRegistry;
    private final RoleRegistry roleRegistry;
    private final AuditLogger auditLogger;
    private final MaterializedViewCache viewCache;

    public AdminController(UserRegistry userRegistry,
                           RoleRegistry roleRegistry,
                           AuditLogger auditLogger,
                           QueryEngine engine) {
        this.userRegistry = userRegistry;
        this.roleRegistry = roleRegistry;
        this.auditLogger = auditLogger;
        this.viewCache = engine.viewCache();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('DBA_ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest request) {
        if (roleRegistry.listAll().stream().noneMatch(r -> r.name().equals(request.roleName()))) {
            return ResponseEntity.badRequest()
                .body("Role not found: '" + request.roleName() + "'");
        }
        userRegistry.register(request.username(), request.password(), request.roleName());
        return ResponseEntity.ok("User '" + request.username() + "' created with role '" + request.roleName() + "'");
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('DBA_ADMIN')")
    public ResponseEntity<?> listUsers() {
        return ResponseEntity.ok(
            userRegistry.listAll().stream()
                .map(u -> new UserSummary(u.username(), u.roleName()))
                .toList()
        );
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('DBA_ADMIN')")
    public ResponseEntity<?> listRoles() {
        return ResponseEntity.ok(
            roleRegistry.listAll().stream()
                .map(r -> new RoleSummary(r.name(), r.storePermissions().keySet(),
                    r.anonymizationPolicyName()))
                .toList()
        );
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('DBA_ADMIN')")
    public ResponseEntity<List<AuditRecord>> getAuditLog(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(auditLogger.getLastN(limit));
    }

    @GetMapping("/views")
    @PreAuthorize("hasRole('DBA_ADMIN')")
    public ResponseEntity<?> getViewStats() {
        return ResponseEntity.ok(viewCache.stats());
    }

    @DeleteMapping("/views")
    @PreAuthorize("hasRole('DBA_ADMIN')")
    public ResponseEntity<String> clearViews() {
        int before = viewCache.viewCount();
        viewCache.clear();
        return ResponseEntity.ok("Cleared " + before + " materialized view(s)");
    }

    private record UserSummary(String username, String roleName) {}
    private record RoleSummary(String name, java.util.Set<String> stores, String anonymizationPolicy) {}
}
