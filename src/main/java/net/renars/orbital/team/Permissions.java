package net.renars.orbital.team;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.utils.Registry;
import net.renars.orbital.utils.Serializable;

import java.util.HashMap;

public class Permissions implements Serializable {
    private final Registry<String, Permission> permissions = new Registry<String, Permission>()
            .keyMapper(Permission.class, Permission::name);

    public Permissions() {
        registerDefaults();
    }

    public Permissions(HashMap<String, Boolean> permissions) {
        registerDefaults();
        for (var entry : permissions.entrySet()) {
            modifyPermission(entry.getKey(), entry.getValue());
        }
    }

    public HashMap<String, Boolean> toMap() {
        var map = new HashMap<String, Boolean>();
        for (var permission : permissions.values()) map.put(permission.name(), permission.value());
        return map;
    }

    public Permissions overlay(Permissions other) {
        var merged = new Permissions(other.toMap());
        for (var permission : permissions.values()) {
            merged.modifyPermission(permission.name(), permission.value());
        }
        return merged;
    }

    private void registerDefaults() {
        OPermissions.all().forEach(permission -> addPermission(permission.name(), new Permission(permission.name(), permission.defaultValue())));
    }

    private void addPermission(String name, Permission defaultValue) {
        permissions.register(name, defaultValue);
    }

    public void modifyPermission(String name, boolean value) {
        if (!permissions.containsKey(name)) return;
        var permission = permissions.get(name);
        permissions.force(name, permission.with(value));
    }

    public Permissions modifyPermissions(HashMap<String, Boolean> permissions) {
        for (var entry : permissions.entrySet()) {
            modifyPermission(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public boolean has(Permission permission) {
        return permissions.containsMapped(permission) && permissions.getMapped(permission).value();
    }

    @Override
    public DataHolder serialize() {
        var holder = new DataHolder();
        for (var permission : permissions.values()) holder.putBoolean(permission.name(), permission.value());
        return holder;
    }

    public static Permissions from(DataHolder holder) {
        var permissions = new Permissions();
        for (var permission : permissions.permissions.values()) {
            if (!holder.containsKey(permission.name())) continue;
            permissions.modifyPermission(permission.name(), holder.getBoolean(permission.name()));
        }
        return permissions;
    }
}
