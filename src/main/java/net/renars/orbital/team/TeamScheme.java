package net.renars.orbital.team;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.DataLoader;
import net.renars.orbital.utils.RepoScheme;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;

@Component
public class TeamScheme implements RepoScheme<Team> {
    static final String NAME = "Teams";
    static final DataLoader<Team> loader = new DataLoader<Team>()
            .deserializer(data -> {
                var id = data.getLong("id");
                var name = data.getString("name");
                var teamMembers = new HashMap<Long, Team.UserDetails>();
                var membersCompound = data.getCompound("members");
                for (var entry : membersCompound.toMap().entrySet()) {
                    var userID = Long.parseLong(entry.getKey());
                    var details = Team.UserDetails.from(DataHolder.from(entry.getValue().m()));
                    teamMembers.put(userID, details);
                }
                var invited = data.getList("invitedUsers", DataHolder::toLong);
                if (teamMembers.isEmpty()) return null;
                return new Team(id, name).addMembers(teamMembers).addInvitedUsers(invited);
            })
            .validator()
            .add("id", DataHolder::getLong)
            .add("name", DataHolder::getString)
            .add("members", DataHolder::getCompound)
            .withDefault("invitedUsers", DataHolder::getList, (holder, id) -> holder.putList(id, new ArrayList<>()))
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
