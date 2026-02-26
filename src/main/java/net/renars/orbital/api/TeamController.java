package net.renars.orbital.api;

import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Komandu (publiskais) API.
 * --Renars
 */
@RestController
@RequestMapping("/team")
public class TeamController implements Controller {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Autowired
    public TeamController(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
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
        if (teamRepository.fromUser(user.get()).size() >= 16) return badRequest("Max team size reached! | Max 16 teams per user");
        var team = teamRepository.createTeam(teamName, user.get());
        return ok("Team created | ID: %s : Name: %s".formatted(team.id(), team.getName()));
    }

    @PostMapping("/{teamID}/invite")
    public ResponseEntity<String> inviteTeammate(
            @PathVariable long teamID,
            @RequestBody InviteTeammate request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        var teamOpt = teamRepository.byID(teamID);
        if (teamOpt.isEmpty()) return badRequest("Team not found");
        var team = teamOpt.get();
        if (!team.isTeamMember(user.get())) return badRequest("You are not a member of this team");
        var teammateOpt = userRepository.byUsername(request.username);
        if (teammateOpt.isEmpty()) return badRequest("User not found");
        if (team.isInvited(teammateOpt.get())) return badRequest("User is already invited to this team");
        var teammate = teammateOpt.get();
        team.createInviteFor(teammate);
        teamRepository.saveToDB(team);
        return ok("Teammate invited!");
    }

    @GetMapping("/{teamID}")
    public ResponseEntity<TeamInfo> getTeamInfo(
            @PathVariable long teamID
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest(null);
        var teamOpt = teamRepository.byID(teamID);
        if (teamOpt.isEmpty()) return badRequest(null);
        if (!teamOpt.get().isTeamMember(user.get())) return badRequest(null);
        var team = teamOpt.get();
        var members = team.teamMembers(userRepository).stream()
                .map(User::getDisplayName)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(new TeamInfo(team.id(), team.getName(), members));
    }

    @GetMapping("/{teamID}/join")
    public ResponseEntity<String> joinTeam(
            @PathVariable long teamID
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        var teamOpt = teamRepository.byID(teamID);
        if (teamOpt.isEmpty()) return badRequest("Team not found");
        var team = teamOpt.get();
        if (!team.isInvited(user.get())) return badRequest("You are not invited to this team");
        team.addMember(user.get(), team.getRole("member"));
        userRepository.saveToDB(user.get());
        teamRepository.saveToDB(team);
        return ok("Joined team!");
    }

    @GetMapping("/teams")
    public ResponseEntity<String> getTeams() {
        var teams = teamRepository.getAll().entrySet().stream()
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

    public record TeamInfo(
            long id,
            String name,
            Set<String> members
    ) {}

    public record RegisterTeamRequest(String name) { }

    public record InviteTeammate(String username) { }
}
