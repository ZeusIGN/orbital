package net.renars.orbital.team;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.utils.Registry;
import net.renars.orbital.utils.Serializable;

import java.util.Collection;

public class Roles implements Serializable {
    private final Registry<String, Team.Role> roles = new Registry<>();

    public Roles withDefault(Team.Role role) {
        addRole(role);
        return this;
    }

    public boolean addRole(Team.Role role) {
        if (roles.containsKey(role.name())) return false;
        roles.register(role.name(), role);
        return true;
    }

    public void forceRole(Team.Role role) {
        roles.force(role.name(), role);
    }

    public void removeRole(String name) {
        roles.unregister(name);
    }

    public Team.Role getRole(String name) {
        return roles.get(name);
    }

    public Collection<Team.Role> getRoles() {
        return roles.values();
    }

    @Override
    public DataHolder serialize() {
        var holder = new DataHolder();
        for (var role : roles.values()) holder.putCompound(role.name(), role.serialize());
        return holder;
    }

    public void deserialize(DataHolder holder) {
        if (holder.toMap().isEmpty()) return;
        roles.reset();
        for (var key : holder.toMap().keySet()) {
            var roleHolder = holder.getCompound(key);
            var role = Team.Role.from(roleHolder);
            this.addRole(role);
        }
    }
}
