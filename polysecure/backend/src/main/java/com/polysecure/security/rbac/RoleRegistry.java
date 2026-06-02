package com.polysecure.security.rbac;

import com.polysecure.security.model.ColumnPolicy;
import com.polysecure.security.model.Permission;
import com.polysecure.security.model.Role;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoleRegistry {

    private final ConcurrentHashMap<String, Role> roles = new ConcurrentHashMap<>();

    public RoleRegistry() {
        // dba_admin: all permissions on all stores, no anonymization
        roles.put("dba_admin", new Role(
            "dba_admin",
            Map.of("*", EnumSet.allOf(Permission.class)),
            Map.of(),
            null
        ));

        // analista_dados: SELECT only on all stores, LGPD anonymization applied
        roles.put("analista_dados", new Role(
            "analista_dados",
            Map.of("*", EnumSet.of(Permission.SELECT, Permission.TRAVERSE)),
            Map.of(),
            "lgpd_compliance"
        ));
    }

    public Role getRole(String name) {
        Role role = roles.get(name);
        if (role == null) throw new IllegalStateException("Role not found: '" + name + "'");
        return role;
    }

    public void register(Role role) {
        roles.put(role.name(), role);
    }

    public Collection<Role> listAll() {
        return roles.values();
    }
}
