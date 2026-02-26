package net.renars.orbital.services;

import lombok.NonNull;
import net.renars.orbital.user.User;
import net.renars.orbital.utils.Result;
import net.renars.orbital.utils.Unique;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserRepository extends Repository<User> {
    public UserRepository(DynamoDbClient client) {
        super(Schemes.USER_SCHEME, client);
    }

    public Result<User> registerUser(
            String email,
            String username,
            String unencryptedPassword,
            String displayName
    ) {
        if (byUsername(username).isPresent()) return Result.error("User with this username already exists!");
        //if (byEmail(email).isPresent()) return Result.error("User with this email already exists!");
        var hashedPassword = BCrypt.hashpw(unencryptedPassword, BCrypt.gensalt());
        var registerStamp = System.currentTimeMillis();
        var user = new User(
                nextID(),
                registerStamp,
                email,
                hashedPassword,
                username,
                displayName,
                Set.of()
        );
        loadToStorage(user.id(), user);
        saveToDB(user);
        return Result.ok(user);
    }

    public boolean isValid(String username, String password) {
        var userOpt = byUsername(username);
        if (userOpt.isEmpty()) return false;
        var user = userOpt.get();
        return BCrypt.checkpw(password, user.getPassword());
    }

    // priekšs dynamoDB load --Renars
    public void addUser(User user) {
        if (containsKey(user.id())) return;
        loadToStorage(user.id(), user);
    }

    public void removeUser(long id) {
        delete(id);
    }

    public void removeUser(User user) {
        removeUser(user.id());
    }

    public Optional<User> byUsername(@Unique @NonNull String username) {
        return filterBy((user) -> user.getUsername().equals(username));
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
