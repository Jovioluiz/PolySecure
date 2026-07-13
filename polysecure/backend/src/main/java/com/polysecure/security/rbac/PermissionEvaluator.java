/*
 * Copyright (c) 2026 Jóvio Luiz Giacomolli
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0
 */

package com.polysecure.security.rbac;

import com.polysecure.catalog.MetadataCatalog;
import com.polysecure.model.*;
import com.polysecure.security.model.Permission;
import com.polysecure.security.model.Role;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PermissionEvaluator {

    private final MetadataCatalog catalog;

    public PermissionEvaluator(MetadataCatalog catalog) {
        this.catalog = catalog;
    }

    public void check(Role role, Statement stmt) {
        switch (stmt) {
            case SelectStatement      s  -> checkStores(role, Permission.SELECT, selectStores(s));
            case InsertStatement      i  -> checkStores(role, Permission.INSERT, insertStores(i));
            case InsertSelectStatement is -> {
                checkStores(role, Permission.SELECT, selectStores(is.source()));
                checkStores(role, Permission.INSERT, insertSelectTargetStores(is));
            }
            case UpdateStatement      u  -> checkStores(role, Permission.UPDATE, updateStores(u));
            case DeleteStatement      d  -> checkStores(role, Permission.DELETE, catalogStores(d.tableName()));
            case CreateTableStatement c  -> checkStores(role, Permission.DDL,    createStores(c));
            case DropTableStatement   dr -> checkStores(role, Permission.DDL,    catalogStores(dr.tableName()));
            case UnionStatement       u  -> {
                checkStores(role, Permission.SELECT, selectStores(u.left()));
                checkStores(role, Permission.SELECT, selectStores(u.right()));
            }
            case ExceptStatement      e  -> {
                checkStores(role, Permission.SELECT, selectStores(e.left()));
                checkStores(role, Permission.SELECT, selectStores(e.right()));
            }
            case RegisterStoreStatement ignored -> checkStores(role, Permission.DDL, List.of());
            case CreateIndexStatement   ci      -> checkStores(role, Permission.DDL, List.of(ci.storeName()));
            case AlterTableStatement    at      -> checkStores(role, Permission.DDL,
                at.operation() == AlterTableStatement.AlterOp.ADD_COLUMN
                    ? List.of(at.addColumn().store()) : List.of(at.dropStoreName()));
        }
    }

    public List<String> extractStores(Statement stmt) {
        return switch (stmt) {
            case SelectStatement      s  -> selectStores(s);
            case InsertStatement      i  -> insertStores(i);
            case InsertSelectStatement is -> Stream.concat(
                selectStores(is.source()).stream(),
                insertSelectTargetStores(is).stream()
            ).distinct().collect(Collectors.toList());
            case UpdateStatement      u  -> updateStores(u);
            case DeleteStatement      d  -> catalogStores(d.tableName());
            case CreateTableStatement c  -> createStores(c);
            case DropTableStatement   dr -> catalogStores(dr.tableName());
            case UnionStatement       u  -> Stream.concat(
                selectStores(u.left()).stream(), selectStores(u.right()).stream()
            ).distinct().collect(Collectors.toList());
            case ExceptStatement      e  -> Stream.concat(
                selectStores(e.left()).stream(), selectStores(e.right()).stream()
            ).distinct().collect(Collectors.toList());
            case RegisterStoreStatement ignored -> List.of();
            case CreateIndexStatement   ci      -> List.of(ci.storeName());
            case AlterTableStatement    at      ->
                at.operation() == AlterTableStatement.AlterOp.ADD_COLUMN
                    ? List.of(at.addColumn().store()) : List.of(at.dropStoreName());
        };
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<String> selectStores(SelectStatement s) {
        return Stream.concat(
            Stream.of(s.from().store()),
            s.joins().stream().map(j -> j.table().store())
        ).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private List<String> insertStores(InsertStatement i) {
        return i.clauses().stream()
            .map(StoreInsertClause::storeName)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<String> insertSelectTargetStores(InsertSelectStatement is) {
        return is.targets().stream()
            .map(StoreTargetClause::storeName)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<String> updateStores(UpdateStatement u) {
        return u.sets().stream()
            .map(SetClause::store)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<String> createStores(CreateTableStatement c) {
        return c.columns().stream()
            .map(ColumnDefinition::store)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<String> catalogStores(String tableName) {
        try {
            return new ArrayList<>(catalog.getStores(tableName));
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    private void checkStores(Role role, Permission op, List<String> stores) {
        Map<String, Set<Permission>> perms = role.storePermissions();
        Set<Permission> wildcard = perms.getOrDefault("*", Set.of());
        for (String store : stores) {
            Set<Permission> storePerms = perms.getOrDefault(store, wildcard);
            if (!storePerms.contains(op)) {
                throw new ForbiddenException(
                    "Role '" + role.name() + "' lacks permission " + op + " on store '" + store + "'");
            }
        }
    }
}
