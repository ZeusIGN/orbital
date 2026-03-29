package net.renars.orbital.team;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.Entity;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.user.User;
import net.renars.orbital.utils.Result;
import net.renars.orbital.utils.Serializable;
import net.renars.orbital.workspace.TeamWorkspace;
import net.renars.orbital.workspace.Workspace;
import net.renars.orbital.workspace.WorkspaceHolder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.stream.Collectors;

public class Team implements Entity, WorkspaceHolder<TeamWorkspace> {
    @Getter
    private final long id;
    @Getter
    private final long ownerID;
    private final HashMap<Long, Member> members = new HashMap<>();
    private final Set<Long> invitedUsers = new HashSet<>();
    private final Set<TeamWorkspace> workspaces = new HashSet<>();
    private final Roles roles = new Roles()
            .withDefault(new Role("default", new Permissions(), 0));
    private final Role DUMMY_ROLE = new Role("DUMMY", new Permissions(), 100);
    private final Role OWNER_ROLE = new Role("Owner", new Permissions(), 0);
    @Getter
    @Setter
    private String name = "";

    public Team(
            long id,
            long ownerID,
            String name
    ) {
        this.id = id;
        this.ownerID = ownerID;
        this.name = name;
    }

    public Roles roles() {
        return roles;
    }

    public Role getRole(String name) {
        return roles.getRole(name);
    }

    public Collection<Role> getRoles() {
        return roles.getRoles();
    }

    public boolean isOwner(User user) {
        return user.id() == ownerID;
    }

    public void createInviteFor(User user) {
        invitedUsers.add(user.id());
        user.getNotifications().addMessage("teamInvite", "Team Invite", "You have been invited to join the team " + this.name, "/teams/" + id + "/join");
    }

    public boolean roleExists(String name) {
        return roles.getRole(name) != null;
    }

    public boolean addRole(User modifier, String name, Permissions permissions) {
        var modifierRole = getRoleOf(modifier);
        return roles.addRole(new Role(name, permissions, modifierRole.priority + 1));
    }

    public void removeRole(String name) {
        if (!roleExists(name)) return;
        roles.removeRole(name);
        membersWithRole(name)
                .forEach((id, member) -> members.put(
                        id,
                        new Member(
                                member.username(),
                                null,
                                member.permissions(),
                                member.additionalInfo()
                        )
                ));
        workspaces.forEach(workspace -> workspace.removeAllowedRole(name));
    }

    public Result<String> modifyRolePriority(User modifier, String name, int newPriority) {
        if (!roleExists(name)) return Result.error("Role does not exist");
        var role = getRole(name);
        if (role == null) return Result.error("Role does not exist");
        var modifierRole = getRoleOf(modifier);
        var owner = isOwner(modifier);
        if (!owner && modifierRole.priority() >= role.priority()) return Result.error("You cannot modify the priority of this role");
        if (!owner && modifierRole.priority() >= newPriority) return Result.error("You cannot set the priority to this value");
        roles.forceRole(new Role(name, role.permissions(), newPriority));
        return Result.ok("Role priority modified");
    }

    public Result<String> modifyRolePermissions(User modifier, String name, HashMap<String, Boolean> permissions) {
        if (!roleExists(name)) return Result.error("Role does not exist");
        var role = getRole(name);
        if (role == null) return Result.error("Role does not exist");
        var temp = OPermissions.allMapped();
        var prev = role.permissions().toMap();
        var passedPermissions = new HashMap<>(prev);
        permissions.forEach((permissionName, value) -> {
            if (temp.get(permissionName) == null) return;
            var permission = temp.get(permissionName);
            if (permission.requirements().isAdminOnly() && !isAdmin(modifier)) return;
            if (permission.requirements().isOwnerOnly() && !isOwner(modifier)) return;
            passedPermissions.put(permissionName, value);
        });
        var modifiedPermissions = role.permissions().modifyPermissions(passedPermissions);
        roles.forceRole(new Role(name, modifiedPermissions, role.priority()));
        return Result.ok("Role permissions modified");
    }

    public HashMap<Long, Member> membersWithRole(String role) {
        return members.entrySet().stream().filter(entry -> {
            var details = entry.getValue();
            return details.role() != null && details.role().equals(role);
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
    }

    public List<User> users(UserRepository userRepository) {
        return members.keySet().stream()
                .map(userRepository::byID)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public HashMap<Long, Member> members() {
        return new HashMap<>(members);
    }

    public void addMember(User user, Member details) {
        members.put(user.id(), details);
        invitedUsers.remove(user.id());
        user.addTeam(id);
    }

    public Member getMember(User user) {
        return members.get(user.id());
    }

    public Role getRoleOf(User user) {
        if (isOwner(user)) return OWNER_ROLE;
        var member = getMember(user);
        if (member == null || member.role() == null) return DUMMY_ROLE;
        return getRole(member.role());
    }

    public Permissions getPermissionsOf(User user) {
        var member = getMember(user);
        if (member == null) return null;
        return member.permissions(this);
    }

    public boolean hasPermission(User user, Permission permission) {
        if (isAdmin(user)) return true;
        var permissions = getPermissionsOf(user);
        return permissions != null && permissions.has(permission);
    }

    public void addMember(User user, Role role) {
        addMember(user, new Member(user.getUsername(), role == null ? null : role.name(), null, new AdditionalInfo(user.id() == ownerID)));
    }

    public void setPermissions(User user, Permissions permissions) {
        var details = members.get(user.id());
        if (details != null)
            members.put(user.id(), new Member(details.username(), details.role(), permissions, details.additionalInfo()));
    }

    public void setRole(User user, String role) {
        if (role != null && !role.isBlank() && !roleExists(role)) return;
        var details = members.get(user.id());
        if (details != null)
            members.put(user.id(), new Member(details.username(), role == null || role.isBlank() ? null : role, details.permissions(this), details.additionalInfo()));
    }

    public boolean isAdmin(User user) {
        var permissions = getPermissionsOf(user);
        var admin = permissions != null && permissions.has(OPermissions.ADMIN);
        return isOwner(user) || admin;
    }

    public Team addMembers(HashMap<Long, Member> users) {
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
        compound.putLong("ownerID", ownerID);
        compound.putString("name", name);
        var membersCompound = new DataHolder();
        for (var entry : members.entrySet()) {
            var id = entry.getKey();
            var details = entry.getValue();
            membersCompound.putCompound(String.valueOf(id), details.serialize());
        }
        compound.putList("invitedUsers", invitedUsers.stream().toList(), (id) -> AttributeValue.builder().n(id + "").build());
        compound.putCompound("members", membersCompound);
        compound.putCompound("roles", roles.serialize());
        serializeWorkspaces(compound);
        return compound;
    }

    public void loadAdditional(DataHolder data) {
        var workspacesData = deserializeWorkspaces(data);
        if (workspacesData != null) addWorkspaces(workspacesData);
        var rolesData = data.getCompound("roles");
        if (rolesData != null) roles.deserialize(rolesData);
    }

    @Override
    public Set<Workspace> combinedWorkspaces(UserRepository userRepository, TeamRepository teamRepository) {
        return new HashSet<>(workspaces);
    }

    @Override
    public Set<Workspace> workspaces() {
        return new HashSet<>(workspaces);
    }

    @Override
    public TeamWorkspace create(String workspaceID, String name) {
        return new TeamWorkspace(workspaceID, name, this.id());
    }

    public void addWorkspace(TeamWorkspace workspace) {
        workspaces.add(workspace);
        workspace.setupPredicate((user) -> {
            if (isAdmin(user)) return true;
            var role = getRoleOf(user);
            return workspace.isRoleAllowed(role.name());
        });
    }

    public void addWorkspaces(Collection<TeamWorkspace> workspaces) {
        workspaces.forEach(this::addWorkspace);
    }

    public boolean isInvited(User user) {
        return invitedUsers.contains(user.id());
    }

    public record Role(
            String name,
            Permissions permissions,
            int priority // the lower the priority, the more power the role has. --Renars
    ) implements Serializable {
        @Override
        public DataHolder serialize() {
            var holder = new DataHolder();
            holder.putString("name", name);
            holder.putCompound("permissions", permissions.serialize());
            holder.putNumber("priority", priority);
            return holder;
        }

        public static Role from(DataHolder holder) {
            var name = holder.getString("name");
            var permissions = Permissions.from(holder.getCompound("permissions"));
            var priority = holder.getInteger("priority", 0);
            return new Role(name, permissions, priority);
        }
    }

    public record Member(
            String username,
            @Nullable String role,
            @Nullable Permissions permissions,
            AdditionalInfo additionalInfo
    ) implements Serializable {
        public Permissions permissions(Team team) {
            if (permissions != null && role != null) return permissions.overlay(team.getRole(role).permissions());
            if (permissions != null) return permissions;
            if (role != null) return team.getRole(role).permissions();
            return new Permissions();
        }

        @Override
        public DataHolder serialize() {
            var holder = new DataHolder();
            if (role != null) holder.putString("role", role);
            holder.putString("username", username);
            if (permissions != null) holder.putCompound("permissions", permissions.serialize());
            holder.putCompound("additionalInfo", additionalInfo.serialize());
            return holder;
        }

        public static Member from(DataHolder holder) {
            var role = holder.containsKey("role") ? holder.getString("role") : null;
            var permissions = holder.containsKey("permissions") ? Permissions.from(holder.getCompound("permissions")) : null;
            var username = holder.getString("username");
            var additionalInfo = holder.containsKey("additionalInfo") ? AdditionalInfo.from(holder.getCompound("additionalInfo")) : new AdditionalInfo(false);
            return new Member(username, role, permissions, additionalInfo);
        }
    }

    public record AdditionalInfo(
            boolean teamOwner
    ) implements Serializable {
        @Override
        public DataHolder serialize() {
            var holder = new DataHolder();
            holder.putBoolean("teamOwner", teamOwner);
            return holder;
        }

        public static AdditionalInfo from(DataHolder holder) {
            var teamOwner = holder.getBoolean("teamOwner");
            return new AdditionalInfo(teamOwner);
        }
    }
}
