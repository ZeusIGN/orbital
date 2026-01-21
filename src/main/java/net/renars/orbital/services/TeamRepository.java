package net.renars.orbital.services;

import net.renars.orbital.team.Team;
import net.renars.orbital.user.User;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

@Service
public class TeamRepository extends Repository<Team> {
    final UserRepository users;

    public TeamRepository(UserRepository repository, DynamoDbClient client) {
        super(Schemes.TEAM_SCHEME, client);
        this.users = repository;
    }

    public Optional<Team> byID(long id) {
        return get(id);
    }

    public Optional<Team> fromUserID(long id) {
        var user = users.byID(id).orElse(null);
        if (user == null || !user.inTeam()) return Optional.empty();
        return fromUser(user);
    }

    public Optional<Team> fromUser(User user) {
        return user.inTeam() ? get(user.getTeamID()) : Optional.empty();
    }
}
