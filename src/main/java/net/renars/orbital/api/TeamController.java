package net.renars.orbital.api;

import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Komandu (publiskais) API.
 * --Renars
 */
@RestController
@RequestMapping("/team")
public class TeamController implements Controller {
    private final TeamRepository teamService;
    private final UserRepository userRepository;

    @Autowired
    public TeamController(TeamRepository teamService, UserRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<String> createTeam(
            @RequestBody RegisterTeamRequest request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        var teamName = request.name.trim();
        if (teamName.isBlank()) return badRequest("Team name cannot be blank");
        if (teamService.fromUser(user.get()).size() >= 16) return badRequest("Max team size reached! | Max 16 teams per user");
        var team = teamService.createTeam(teamName, user.get());
        return ok("Team created | ID: %s : Name: %s".formatted(team.id(), team.getName()));
    }

    @GetMapping("/teams")
    public ResponseEntity<String> getTeams() {
        var teams = teamService.getAll().entrySet().stream()
                .map(entry -> {
                    var id = entry.getKey();
                    var team = entry.getValue();
                    return "ID: %s | Name: %s | Members: %s".formatted(id, team.getName(),
                            team.teamMembers(userRepository).stream().map(User::getDisplayName)
                            .reduce((a, b) -> a + ", " + b).orElse("No members")
                    );
                })
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No teams found");
        return ok(teams);
    }

    record RegisterTeamRequest(String name) { }
}
