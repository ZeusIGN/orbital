package net.renars.orbital.team;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.DataLoader;
import net.renars.orbital.utils.RepoScheme;
import org.springframework.stereotype.Component;

@Component
public class TeamScheme implements RepoScheme<Team> {
    static final String NAME = "Teams";
    static final DataLoader<Team> loader = new DataLoader<Team>()
            .deserializer(data -> {
                var id = data.getLong("id");
                var teamMembers = data.getList("teamMembers", DataHolder::toLong);
                return new Team(id).addMembers(teamMembers);
            })
            .validator()
            .add("id", DataHolder::getLong)
            .add("teamMembers", DataHolder::getList)
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
