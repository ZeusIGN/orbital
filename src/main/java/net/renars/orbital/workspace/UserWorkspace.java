package net.renars.orbital.workspace;

import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;

public class UserWorkspace extends Workspace {
    private final long userID;

    public UserWorkspace(String id, String name, long userID) {
        super(id, name);
        this.userID = userID;
    }

    @Override
    public void save(TeamRepository teamRepository, UserRepository userRepository) {
        var user = userRepository.get(userID);
        if (user.isEmpty()) return;
        userRepository.saveToDB(user.get());
    }
}
