package net.renars.orbital.api;

import net.renars.orbital.services.JwtService;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.services.WrappedUserService;
import net.renars.orbital.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WrappedUserService userDetailsService;

    @Autowired
    public UserController(UserRepository userService, JwtService jwtService, AuthenticationManager authenticationManager, WrappedUserService userDetailsService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
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
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        var username = request.username();
        var password = request.password();
        if (username.isBlank()) return LoginResponse.badRequest("Username cannot be blank");
        if (password.isBlank()) return LoginResponse.badRequest("Password cannot be blank");

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (Exception e) {
            return LoginResponse.badRequest("Invalid username or password");
        }

        var userDetails = userDetailsService.loadUserByUsername(username);
        var jwtToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);

        return buildLoginResponse(jwtToken, refreshToken);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return LoginResponse.badRequest("Invalid Refresh Token");
        }
        final var refreshToken = authHeader.substring(7);
        final String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            return LoginResponse.badRequest("Invalid Refresh Token");
        }

        if (username == null) return LoginResponse.badRequest("Invalid Refresh Token");
        var userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(refreshToken, userDetails))
            return LoginResponse.badRequest("Invalid Refresh Token");
        var accessToken = jwtService.generateToken(userDetails);
        return buildLoginResponse(accessToken, refreshToken);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        var userOpt = getAuthUser();
        if (userOpt.isEmpty()) return badRequest();
        var user = userOpt.get();
        return ok(new UserResponse(user.id(), user.getUsername(), user.getDisplayName()));
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logout() {
        var clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    // debug --Renars
    @GetMapping("/users")
    public ResponseEntity<String> getUsers() {
        return ok(userService.getAllUsers().stream()
                .map(user -> "%s | %s | %s\n".formatted(user.getDisplayName(), user.getUsername(), user.id()))
                .collect(Collectors.joining())
        );
    }

    private ResponseEntity<LoginResponse> buildLoginResponse(String accessToken, String refreshToken) {
        var refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(accessToken, refreshToken, null));
    }

    public record LoginRequest(
            String username,
            String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String errorMessage
    ) {
        private static ResponseEntity<LoginResponse> badRequest(String message) {
            return ResponseEntity.badRequest().body(new LoginResponse(null, null, message));
        }
    }

    public record UserResponse(
            long id,
            String username,
            String displayName
    ) {
    }

    public record RegisterRequest(
            String email,
            String password,
            String username,
            String displayName
    ) {
    }
}
