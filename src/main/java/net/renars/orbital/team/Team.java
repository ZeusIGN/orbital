package net.renars.orbital.team;

import net.renars.orbital.services.UserRepository;
import net.renars.orbital.user.User;
import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.Entity;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Team implements Entity {
    private final long id;
    private final List<Long> members = new ArrayList<>();

    public Team(long id) {
        this.id = id;
    }

    public List<User> teamMembers(UserRepository userRepository) {
        return members.stream()
                .map(userRepository::byID)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public void addMember(long id) {
        members.add(id);
    }

    public Team addMembers(List<Long> ids) {
        members.addAll(ids);
        return this;
    }

    public boolean isTeamMember(long id) {
        return members.contains(id);
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
        compound.putList("teamMembers", members.stream()
                .map((val) -> AttributeValue.builder().n(val + "").build())
                .collect(Collectors.toList()));
        return compound;
    }
}
