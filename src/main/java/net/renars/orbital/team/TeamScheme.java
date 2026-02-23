package net.renars.orbital.team;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.DataLoader;
import net.renars.orbital.utils.RepoScheme;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class TeamScheme implements RepoScheme<Team> {
    static final String NAME = "Teams";
    static final DataLoader<Team> loader = new DataLoader<Team>()
            .deserializer(data -> {
                var id = data.getLong("id");
                var name = data.getString("name");
                HashMap<Long, Team.UserDetails> teamMembers = new HashMap<>();
                var membersCompound = data.getCompound("members");
                for (var entry : membersCompound.toMap().entrySet()) {
                    var userID = Long.parseLong(entry.getKey());
                    var details = Team.UserDetails.from(DataHolder.from(entry.getValue().m()));
                    teamMembers.put(userID, details);
                }
                return new Team(id, name).addMembers(teamMembers);
            })
            .validator()
            .add("id", DataHolder::getLong)
            .add("name", DataHolder::getString)
            .add("members", DataHolder::getCompound)
            .build();

    @Override
    public String tableName() {
        return NAME;
    }

    @Override
    public DataLoader<Team> loader() {
        return loader;
    }
}
