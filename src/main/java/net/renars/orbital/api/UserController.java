package net.renars.orbital.api;

import net.renars.orbital.services.UserRepository;
import net.renars.orbital.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Primārais (publiskais) API.
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
        if (!StringUtils.isValidEmail(email)) return badRequest("Invalid email!");
        var result = userService.registerUser(email, name, displayName);
        if (result.isError()) return badRequest(result.errorMsg());
        return ok("User created with ID: " + result.value().id());
    }

    @GetMapping("/users")
    public ResponseEntity<String> getUsers() {
        return ok(userService.getAllUsers().stream()
                .map(user -> "%s | %s | %s\n".formatted(user.getDisplayName(), user.getUsername(), user.id()))
                .collect(Collectors.joining())
        );
    }

    public record RegisterRequest(
            String email,
            String password,
            String username,
            String displayName
    ) {
    }
}

