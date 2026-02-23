package net.renars.orbital.api;

import net.renars.orbital.services.UserRepository;
import net.renars.orbital.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Primārais user (publiskais) API.
 * --Renars
 */
@RestController
@RequestMapping("/user")
public class UserController implements Controller {
    private final UserRepository userService;

    @Autowired
    public UserController(UserRepository userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> createUser(
            @RequestBody RegisterRequest request
    ) {
        var email = request.email();
        var password = request.password();
        var name = request.username();
        var displayName = request.displayName();
        if (email.isBlank() || name.isBlank()) return badRequest("Email or username cannot be blank");
        if (displayName.isBlank()) return badRequest("Display name cannot be blank!");
        if (password.length() < 8) return badRequest("Password must be at least 8 characters long!");
        // 72 byte limits priekšs bcrypt --Renars
        if (password.getBytes().length > 72) return badRequest("Password is too long!");
        if (!StringUtils.isValidEmail(email)) return badRequest("Invalid email!");
        var result = userService.registerUser(email, name, password, displayName);
        if (result.isError()) return badRequest(result.errorMsg());
        return ok("User created with ID: " + result.value().id());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> createUser(
            @RequestBody LoginRequest request
    ) {
        var username = request.username();
        var password = request.password();
        if (username.isBlank()) return LoginResponse.badRequest("Username cannot be blank");
        if (password.isBlank()) return LoginResponse.badRequest("Password cannot be blank");
        if (!userService.isValid(username, password))
            return LoginResponse.badRequest("Invalid username or password");
        return LoginResponse.ok("dummy-token-%s".formatted(username));
    }

    // debug --Renars
    @GetMapping("/users")
    public ResponseEntity<String> getUsers() {
        return ok(userService.getAllUsers().stream()
                .map(user -> "%s | %s | %s\n".formatted(user.getDisplayName(), user.getUsername(), user.id()))
                .collect(Collectors.joining())
        );
    }

    public record LoginRequest(
            String username,
            String password
    ) {

    }

    public record LoginResponse(
            String token,
            String errorMessage
    ) {
        private static ResponseEntity<LoginResponse> badRequest(String message) {
            return ResponseEntity.badRequest().body(new LoginResponse(null, message));
        }

        private static ResponseEntity<LoginResponse> ok(String token) {
            return ResponseEntity.ok(new LoginResponse(token, null));
        }
    }

    public record RegisterRequest(
            String email,
            String password,
            String username,
            String displayName
    ) {
    }
}

