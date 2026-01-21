package net.renars.orbital.user;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.DataLoader;
import net.renars.orbital.utils.RepoScheme;

public class UserScheme implements RepoScheme<User> {
    static final String NAME = "Users";
    static final DataLoader<User> loader = new DataLoader<User>()
            .deserializer(data -> {
                var id = data.getLong("id");
                var signUpStamp = data.getLong("signUpStamp");
                var email = data.getString("email");
                var username = data.getString("username");
                var displayName = data.getString("displayName");
                var teamID = data.getLong("teamID");
                return new User(id, signUpStamp, email, username, displayName, teamID);
            })
            .validator()
            .add("id", DataHolder::getLong)
            .add("signUpStamp", DataHolder::getLong)
            .add("email", DataHolder::getString)
            .add("username", DataHolder::getString)
            .add("displayName", DataHolder::getString)
            .add("teamID", DataHolder::getLong)
            .build();

    @Override
    public String tableName() {
        return NAME;
    }

    @Override
    public DataLoader<User> loader() {
        return loader;
    }
}
