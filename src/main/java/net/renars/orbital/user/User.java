package net.renars.orbital.user;

import lombok.Getter;
import lombok.Setter;
import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.Entity;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.team.Team;
import net.renars.orbital.utils.Serializable;
import net.renars.orbital.utils.Unique;

import java.util.Date;
import java.util.Optional;

public class User implements Entity {
    @Unique
    private final long id;
    @Getter
    private final long signUpStamp;
    @Getter
    private long teamID;
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

    public User(long id, long signUpStamp, String email, String password, String username, String displayName, long teamID) {
        this.id = id;
        this.signUpStamp = signUpStamp;
        this.email = email;
        this.password = password;
        this.username = username;
        this.displayName = displayName;
        this.teamID = teamID;
    }

    public Date signUpDate() {
        return new Date(signUpStamp);
    }

    public long id() {
        return id;
    }

    public void updateTeamID(long id) {
        this.teamID = id;
    }

    public boolean inTeam() {
        return teamID != -1;
    }

    public Optional<Team> getCurrentTeam(TeamRepository repository) {
        if (!inTeam()) return Optional.empty();
        return repository.get(teamID);
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
        compound.putLong("teamID", teamID);
        DataHolder children = new DataHolder();
        for (Serializable child : children()) {
            children.putCompound(child.getClass().getSimpleName(), child.serialize());
        }
        compound.putCompound("children", children);
        return compound;
    }

    enum WebRole {
        VIEWER,
        USER,
        ADMIN
    }
}
