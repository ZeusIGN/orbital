package net.renars.orbital.user;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.data.DataLoader;
import net.renars.orbital.utils.RepoScheme;

import java.util.ArrayList;

public class UserScheme implements RepoScheme<User> {
    static final String NAME = "Users";
    static final DataLoader<User> loader = new DataLoader<User>()
            .deserializer(data -> {
                var id = data.getLong("id");
                var username = data.getString("username");
                var signUpStamp = data.getLong("signUpStamp");
                var email = data.getString("email");
                var password = data.getString("password");
                var displayName = data.getString("displayName");
                var teamID = data.getLong("teamID");
                var user = new User(id, signUpStamp, email, password, username, displayName, teamID);
                user.loadChildren(data);
                return user;
            })
            .validator()
            .add("id", DataHolder::getLong)
            .add("username", DataHolder::getString)
            .add("signUpStamp", DataHolder::getLong)
            .add("email", DataHolder::getString)
            .add("password", DataHolder::getString)
            .add("displayName", DataHolder::getString)
            .add("teamID", DataHolder::getLong)
            .withDefault("children", DataHolder::getList, (holder, id) -> holder.putList(id, new ArrayList<>()))
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
