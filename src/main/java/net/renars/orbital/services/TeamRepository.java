package net.renars.orbital.services;

import net.renars.orbital.team.Team;
import net.renars.orbital.user.User;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;
import java.util.Set;

@Service
public class TeamRepository extends Repository<Team> {
    final UserRepository users;

    public TeamRepository(UserRepository repository, DynamoDbClient client) {
        super(Schemes.TEAM_SCHEME, client);
        this.users = repository;
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
