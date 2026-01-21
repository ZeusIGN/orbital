package net.renars.orbital.services;

import lombok.NonNull;
import net.renars.orbital.user.User;
import net.renars.orbital.utils.Result;
import net.renars.orbital.utils.Unique;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.*;

@Service
public class UserRepository extends Repository<User> {
    public UserRepository(DynamoDbClient client) {
        super(Schemes.USER_SCHEME, client);
    }

    public Result<User> registerUser(String email, String username, String displayName) {
        if (byUsername(username).isPresent()) return Result.error("User with this username already exists!");
        if (byEmail(email).isPresent()) return Result.error("User with this email already exists!");
        long signUpStamp = System.currentTimeMillis();
        User user = new User(nextID(), signUpStamp, email, username, displayName, -1);
        save(user.id(), user);
        saveToDB(user);
        return Result.ok(user);
    }

    // priekšs dynamoDB load --Renars
    public void addUser(User user) {
        if (containsKey(user.id())) return;
        save(user.id(), user);
    }

    public void removeUser(long id) {
        delete(id);
    }

    public void removeUser(User user) {
        removeUser(user.id());
    }

    public Optional<User> byUsername(@Unique @NonNull String name) {
        return filterBy((user) -> user.getUsername().equals(name));
    }

    public Optional<User> byEmail(@Unique @NonNull String email) {
        return filterBy((user) -> user.getEmail().equals(email));
    }

    public Optional<User> byID(long id) {
        return get(id);
    }

    public List<User> getAllUsers() {
        return values().toList();
    }
}
