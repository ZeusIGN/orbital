package net.renars.orbital.api;

import net.renars.orbital.services.JwtService;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.services.WrappedUserService;
import net.renars.orbital.team.Team;
import net.renars.orbital.utils.StringUtils;
import net.renars.orbital.workspace.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Primārais user (publiskais) API.
 * --Renars
 */
@RestController
@RequestMapping("/user")
public class UserController implements Controller {
    private final UserRepository userService;
    private final TeamRepository teamRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WrappedUserService userDetailsService;

    @Autowired
    public UserController(
            UserRepository userService,
            TeamRepository teamRepository,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            WrappedUserService userDetailsService
    ) {
        this.userService = userService;
        this.teamRepository = teamRepository;
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
        if (name.isBlank()) return badRequest("Username cannot be blank");
        if (displayName.isBlank()) return badRequest("Display name cannot be blank!");
        if (password.length() < 8) return badRequest("Password must be at least 8 characters long!");
        // 72 byte limits priekšs bcrypt --Renars
        if (password.getBytes().length > 72) return badRequest("Password is too long!");
        // TODO email support --Renars
        //if (!StringUtils.isValidEmail(email)) return badRequest("Invalid email!");
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

    @GetMapping("/teamInvites")
    public ResponseEntity<TeamInvitesResponse> getTeamInvites() {
        var userOpt = getAuthUser();
        if (userOpt.isEmpty()) return badRequest(null);
        var user = userOpt.get();
        var invites = teamRepository.teamsInvitingUser(user).stream()
                .collect(Collectors.toMap(
                        Team::id,
                        Team::getName,
                        (a, _) -> a,
                        HashMap::new
                ));
        return ok(new TeamInvitesResponse(invites));
    }

    @GetMapping("/teams")
    public ResponseEntity<UserTeamsResponse> getTeams() {
        var userOpt = getAuthUser();
        if (userOpt.isEmpty()) return badRequest(null);
        var user = userOpt.get();
        var teams = user.getActiveTeams(teamRepository).stream()
                .collect(Collectors.toMap(
                        Team::id,
                        Team::getName,
                        (a, _) -> a,
                        HashMap::new
                ));
        return ok(new UserTeamsResponse(teams));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        var userOpt = getAuthUser();
        if (userOpt.isEmpty()) return badRequest();
        var user = userOpt.get();
        return ok(new UserResponse(user.id(), user.getUsername(), user.getDisplayName()));
    }

    @GetMapping("/workspaces")
    public ResponseEntity<WorkspacesResponse> getWorkspaces() {
        var userOpt = getAuthUser();
        if (userOpt.isEmpty()) return badRequest();
        var user = userOpt.get();
        var workspaceIds = user.combinedWorkspaces(userService, teamRepository)
                .stream()
                .collect(Collectors.toMap(
                        workspace -> workspace.getId().toString(),
                        Workspace::getName,
                        (a, _) -> a
                ));
        return ok(new WorkspacesResponse(workspaceIds));
    }

    @GetMapping("/workspace/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(@PathVariable String id) {
        var userOpt = getAuthUser();
        if (userOpt.isEmpty()) return badRequest();
        var user = userOpt.get();
        UUID workspaceId;
        try {
            workspaceId = UUID.fromString(id);
        } catch (Exception e) {
            return badRequest(null);
        }
        var workspaceOpt = user.workspaceByID(workspaceId, userService, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest(null);
        var workspace = workspaceOpt.get();
        return ok(new WorkspaceResponse(workspace.getId(), workspace.getName()));
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

    public record UserTeamsResponse(
            HashMap<Long, String> teams
    ) {
    }

    public record TeamInvitesResponse(
            HashMap<Long, String> invites
    ) {
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

    public record WorkspaceResponse(
            UUID id,
            String name
    ) {
    }

    public record WorkspacesResponse(
            Map<String, String> workspaces
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
