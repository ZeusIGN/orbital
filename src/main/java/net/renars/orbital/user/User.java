package net.renars.orbital.user;

import lombok.Getter;
import lombok.Setter;
import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.Entity;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.team.Team;
import net.renars.orbital.utils.Unique;
import net.renars.orbital.workspace.Workspace;
import net.renars.orbital.workspace.WorkspaceHolder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class User implements Entity, WorkspaceHolder {
    @Unique
    private final long id;
    @Getter
    private final long signUpStamp;
    @Getter
    private final Notifications notifications = new Notifications(this);
    private final Set<Workspace> workspaces = new HashSet<>();
    @Getter
    private Set<Long> activeTeams;
    @Getter
    @Unique
    String email;
    @Getter
    String password;
    @Getter
    @Unique
    String username;
    @Getter
    String displayName;
    @Setter
    @Getter
    WebRole webRole = WebRole.USER;

    public User(long id, long signUpStamp, String email, String password, String username, String displayName, Set<Long> activeTeams) {
        this.id = id;
        this.signUpStamp = signUpStamp;
        this.email = email;
        this.password = password;
        this.username = username;
        this.displayName = displayName;
        this.activeTeams = new HashSet<>(activeTeams);
    }

    public Date signUpDate() {
        return new Date(signUpStamp);
    }

    public long id() {
        return id;
    }

    public void addTeam(long id) {
        activeTeams.add(id);
    }

    public boolean inTeam() {
        return !activeTeams.isEmpty();
    }

    public Set<Team> getActiveTeams(TeamRepository repository) {
        if (!inTeam()) return Set.of();
        return activeTeams.stream()
                .map(repository::byID)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    @Override
    public DataHolder serialize() {
        var compound = new DataHolder();
        compound.putLong("id", id);
        compound.putLong("signUpStamp", signUpStamp);
        compound.putString("email", email);
        compound.putString("password", password);
        compound.putString("username", username);
        compound.putString("displayName", displayName);
        compound.putList("activeTeams", activeTeams.stream().toList(), (id) -> AttributeValue.builder().n(id + "").build());
        compound.putCompound("notifications", notifications.serialize());
        serializeWorkspaces(compound);
        return compound;
    }

    public void loadAdditional(DataHolder data) {
        var notificationsData = data.getCompound("notifications");
        if (notificationsData != null) notifications.deserialize(notificationsData);
        var workspacesData = deserializeWorkspaces(data);
        if (workspacesData != null) workspaces.addAll(workspacesData);
    }

    public void addWorkspace(Workspace workspace) {
        workspaces.add(workspace);
    }

    @Override
    public Set<Workspace> combinedWorkspaces(UserRepository userRepository, TeamRepository teamRepository) {
        var fullSet = new HashSet<>(workspaces);
        getActiveTeams(teamRepository).forEach(team -> fullSet.addAll(team.combinedWorkspaces(userRepository, teamRepository)));
        return fullSet;
    }

    @Override
    public Set<Workspace> workspaces() {
        return new HashSet<>(workspaces);
    }

    public enum WebRole {
        VIEWER,
        USER,
        ADMIN
    }
}
