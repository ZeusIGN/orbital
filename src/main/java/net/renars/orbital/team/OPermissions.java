package net.renars.orbital.team;

import com.google.common.base.Suppliers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OPermissions {
    private static final Supplier<Set<Permission>> allPerms = Suppliers.memoize(OPermissions::memoize);
    public static final Permission MANAGE_TEAM = new Permission("manage_team");
    public static final Permission MANAGE_MEMBERS = new Permission("manage_members");
    public static final Permission MANAGE_PERMISSIONS = new Permission("manage_permissions").markAdminOnly();
    public static final Permission ADMIN = new Permission("admin").markOwnerOnly();

    public static Set<Permission> all() {
        return allPerms.get();
    }

    public static HashMap<String, Permission> allMapped() {
        var map = new HashMap<String, Permission>();
        for (var permission : all()) map.put(permission.name(), permission);
        return map;
    }

    private static Set<Permission> memoize() {
        return Arrays.stream(OPermissions.class.getFields())
                .filter(field -> field.getType() == Permission.class)
                .map(field -> {
                    try {
                        return (Permission) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }
}
