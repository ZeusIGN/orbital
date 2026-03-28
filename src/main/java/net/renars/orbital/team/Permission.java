package net.renars.orbital.team;

import java.util.HashMap;

public record Permission(
        String name,
        boolean value,
        boolean defaultValue,
        Requirements requirements
) {
    public Permission(String name) {
        this(name, false, false, new Requirements());
    }

    public Permission(String name, boolean defaultValue) {
        this(name, defaultValue, defaultValue, new Requirements());
    }

    public Permission with(boolean value) {
        return new Permission(name, value, defaultValue, new Requirements());
    }

    public Permission markAdminOnly() {
        return new Permission(name, value, defaultValue, requirements.adminOnly());
    }

    public Permission markOwnerOnly() {
        return new Permission(name, value, defaultValue, requirements.ownerOnly());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Permission permission && permission.name.equals(name);
    }

    public static class Requirements {
        private boolean adminOnly = false;
        private boolean ownerOnly = false;

        public Requirements() {
        }

        public HashMap<String, Boolean> toMap() {
            var map = new HashMap<String, Boolean>();
            map.put("adminOnly", adminOnly);
            map.put("ownerOnly", ownerOnly);
            return map;
        }

        public Requirements adminOnly() {
            this.adminOnly = true;
            return this;
        }

        public Requirements ownerOnly() {
            this.ownerOnly = true;
            return this;
        }

        public boolean isAdminOnly() {
            return adminOnly;
        }

        public boolean isOwnerOnly() {
            return ownerOnly;
        }
    }
}
