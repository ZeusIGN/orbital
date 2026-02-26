package net.renars.orbital.services;

import net.renars.orbital.team.Team;
import net.renars.orbital.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamRepository extends Repository<Team> {
    final UserRepository users;

    @Autowired
    public TeamRepository(UserRepository repository, DynamoDbClient client) {
        this.users = repository;
        super(Schemes.TEAM_SCHEME, client);
    }

    @Override
    protected void loadedEntity(Team team) {
        team.load(users);
    }

    public Set<Team> teamsInvitingUser(User user) {
        return getAll().values().stream()
                .filter(team -> team.isInvited(user))
                .collect(Collectors.toSet());
    }

    public Team createTeam(String name, User owner) {
        var team = new Team(nextID(), name);
        team.addMember(owner, team.getRole("manager"));
        loadToStorage(team.id(), team);
        saveToDB(team);
        return team;
    }

    public Optional<Team> byID(long teamID) {
        return get(teamID);
    }

    public Set<Team> fromUser(User user) {
        return user.inTeam() ? user.getActiveTeams(this) : Set.of();
    }
}
