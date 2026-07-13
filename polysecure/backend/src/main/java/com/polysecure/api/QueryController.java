/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.api;

import com.polysecure.api.dto.QueryRequest;
import com.polysecure.api.dto.QueryResponse;
import com.polysecure.engine.DmlResult;
import com.polysecure.engine.QueryEngine;
import com.polysecure.model.Statement;
import com.polysecure.security.anonymization.AnonymizationEngine;
import com.polysecure.security.audit.AuditLogger;
import com.polysecure.security.audit.AuditRecord;
import com.polysecure.security.model.AnonymizationPolicy;
import com.polysecure.security.model.PolyUser;
import com.polysecure.security.model.Role;
import com.polysecure.security.rbac.PermissionEvaluator;
import com.polysecure.security.rbac.PolicyRegistry;
import com.polysecure.security.rbac.RoleRegistry;
import com.polysecure.translator.StandardSqlTranslator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/query")
public class QueryController {

    private final QueryEngine engine;
    private final RoleRegistry roleRegistry;
    private final PermissionEvaluator permissionEvaluator;
    private final PolicyRegistry policyRegistry;
    private final AnonymizationEngine anonymizationEngine;
    private final AuditLogger auditLogger;
    private final StandardSqlTranslator translator;

    public QueryController(QueryEngine engine,
                           RoleRegistry roleRegistry,
                           PermissionEvaluator permissionEvaluator,
                           PolicyRegistry policyRegistry,
                           AnonymizationEngine anonymizationEngine,
                           AuditLogger auditLogger,
                           StandardSqlTranslator translator) {
        this.engine = engine;
        this.roleRegistry = roleRegistry;
        this.permissionEvaluator = permissionEvaluator;
        this.policyRegistry = policyRegistry;
        this.anonymizationEngine = anonymizationEngine;
        this.auditLogger = auditLogger;
        this.translator = translator;
    }

    /** Accepts SQL-Poly syntax directly. */
    @PostMapping
    public ResponseEntity<?> execute(@RequestBody QueryRequest request, Authentication auth) {
        return runPipeline(request.sql(), request.sql(), auth);
    }

    /** Accepts standard PostgreSQL SQL; PolySecure translates internally to SQL-Poly. */
    @PostMapping("/standard")
    public ResponseEntity<?> executeStandard(@RequestBody QueryRequest request, Authentication auth) {
        String translated = translator.translate(request.sql());
        return runPipeline(translated, request.sql(), auth);
    }

    /** Returns the SQL-Poly translation without executing — useful for GUI preview. */
    @PostMapping("/translate")
    public ResponseEntity<Map<String, String>> translate(@RequestBody QueryRequest request,
                                                         Authentication auth) {
        String translated = translator.translate(request.sql());
        return ResponseEntity.ok(Map.of("original", request.sql(), "translated", translated));
    }

    private ResponseEntity<?> runPipeline(String sql, String auditSql, Authentication auth) {
        long start = System.currentTimeMillis();
        PolyUser user = (PolyUser) auth.getPrincipal();
        Role role = roleRegistry.getRole(user.roleName());

        Statement stmt = engine.parse(sql);
        permissionEvaluator.check(role, stmt);
        List<String> stores = permissionEvaluator.extractStores(stmt);

        Object result = engine.execute(sql);

        List<String> anonymizedCols = List.of();
        if (result instanceof List<?> rawRows && role.anonymizationPolicyName() != null) {
            Optional<AnonymizationPolicy> policy = policyRegistry.getPolicy(role.anonymizationPolicyName());
            if (policy.isPresent()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) rawRows;
                if (!rows.isEmpty()) anonymizedCols = anonymizationEngine.affectedColumns(rows.get(0), policy.get());
                result = anonymizationEngine.apply(rows, policy.get());
            }
        }

        int rowCount = result instanceof List<?> list ? list.size() : -1;
        auditLogger.log(new AuditRecord(
            Instant.now(), user.username(), role.name(), auditSql,
            stores, rowCount, anonymizedCols, System.currentTimeMillis() - start
        ));

        return switch (result) {
            case List<?> rows -> {
                @SuppressWarnings("unchecked")
                var typed = (List<Map<String, Object>>) rows;
                yield ResponseEntity.ok(new QueryResponse(typed.size(), typed));
            }
            case DmlResult r -> ResponseEntity.ok(r);
            default -> ResponseEntity.ok(result);
        };
    }
}
