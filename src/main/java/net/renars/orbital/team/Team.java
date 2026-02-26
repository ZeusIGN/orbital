package net.renars.orbital.team;

import lombok.Getter;
import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.Entity;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.user.User;
import net.renars.orbital.utils.Serializable;
import net.renars.orbital.workspace.Workspace;
import net.renars.orbital.workspace.WorkspaceHolder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.stream.Collectors;

public class Team implements Entity, WorkspaceHolder {
    @Getter
    private final long id;
    private final HashMap<Long, UserDetails> members = new HashMap<>();
    private final Set<Long> invitedUsers = new HashSet<>();
    private final Set<Workspace> workspaces = new HashSet<>();
    private final HashMap<String, Role> roles = new HashMap<>() {{
        put("manager", new Role("manager", new Permissions(true, true)));
        put("member", new Role("member", new Permissions(false, false)));
    }};
    @Getter
    private String name = "";

    public Team(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Role getRole(String name) {
        return roles.get(name);
    }

    public void createInviteFor(User user) {
        invitedUsers.add(user.id());
        user.getNotifications().addMessage("teamInvite", "Team Invite", "You have been invited to join the team " + this.name, "/teams/" + id + "/join");
    }

    public boolean roleExists(String name) {
        return roles.containsKey(name);
    }

    public void addRole(String name, Permissions permissions) {
        roles.put(name, new Role(name, permissions));
    }

    public void removeRole(String name) {
        if (roles.size() - 1 <= 0) return;
        roles.remove(name);
    }

    public List<User> teamMembers(UserRepository userRepository) {
        return members.keySet().stream()
                .map(userRepository::byID)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public void addMember(User user, UserDetails details) {
        members.put(user.id(), details);
        invitedUsers.remove(user.id());
        user.addTeam(id);
    }

    public void addMember(User user, Role role) {
        addMember(user, new UserDetails(role, null));
    }

    public void overridePermissions(long id, Permissions permissions) {
        var details = members.get(id);
        if (details != null) members.put(id, new UserDetails(details.role(), permissions));
    }

    public Team addMembers(HashMap<Long, UserDetails> users) {
        members.putAll(users);
        return this;
    }

    public Team load(UserRepository userRepository) {
        for (var id : members.keySet()) {
            var userOpt = userRepository.byID(id);
            if (userOpt.isEmpty()) continue;
            var user = userOpt.get();
            user.addTeam(getId());
        }
        return this;
    }

    public Team addInvitedUsers(Collection<Long> users) {
        invitedUsers.addAll(users);
        return this;
    }

    public boolean isTeamMember(long id) {
        return members.containsKey(id);
    }

    public boolean isTeamMember(User user) {
        return isTeamMember(user.id());
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public DataHolder serialize() {
        var compound = new DataHolder();
        compound.putLong("id", id);
        compound.putString("name", name);
        var membersCompound = new DataHolder();
        for (var entry : members.entrySet()) {
            var id = entry.getKey();
            var details = entry.getValue();
            membersCompound.putCompound(String.valueOf(id), details.serialize());
        }
        compound.putList("invitedUsers", invitedUsers.stream().toList(), (id) -> AttributeValue.builder().n(id + "").build());
        compound.putCompound("members", membersCompound);
        return compound;
    }

    @Override
    public Set<Workspace> combinedWorkspaces(UserRepository userRepository, TeamRepository teamRepository) {
        return new HashSet<>(workspaces);
    }

    @Override
    public Set<Workspace> workspaces() {
        return new HashSet<>(workspaces);
    }

    public void addWorkspace(Workspace workspace) {
        workspaces.add(workspace);
    }

    public boolean isInvited(User user) {
        return invitedUsers.contains(user.id());
    }

    public record Role(String name, Permissions permissions) implements Serializable {
        @Override
        public DataHolder serialize() {
            var holder = new DataHolder();
            holder.putString("name", name);
            holder.putCompound("permissions", permissions.serialize());
            return holder;
        }

        public static Role from(DataHolder holder) {
            var name = holder.getString("name");
            var permissions = Permissions.from(holder.getCompound("permissions"));
            return new Role(name, permissions);
        }
    }

    public record UserDetails(Role role, Permissions permissions) implements Serializable {
        @Override
        public DataHolder serialize() {
            var holder = new DataHolder();
            holder.putCompound("role", role.serialize());
            if (permissions != null) holder.putCompound("permissions", permissions.serialize());
            return holder;
        }

        public static UserDetails from(DataHolder holder) {
            var role = Role.from(holder.getCompound("role"));
            var permissions = holder.containsKey("permissions") ? Permissions.from(holder.getCompound("permissions")) : null;
            return new UserDetails(role, permissions);
        }
    }

    public record Permissions(
            boolean canEditTeam,
            boolean canManageMembers
    ) implements Serializable {
        @Override
        public DataHolder serialize() {
            var holder = new DataHolder();
            holder.putBoolean("canEditTeam", canEditTeam);
            holder.putBoolean("canManageMembers", canManageMembers);
            return holder;
        }

        public static Permissions from(DataHolder holder) {
            var canEditTeam = holder.getBoolean("canEditTeam", false);
            var canManageMembers = holder.getBoolean("canManageMembers", false);
            return new Permissions(canEditTeam, canManageMembers);
        }
    }
}
