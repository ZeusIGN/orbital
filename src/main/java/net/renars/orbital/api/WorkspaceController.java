package net.renars.orbital.api;

import jakarta.annotation.Nullable;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.workspace.DateEvent;
import net.renars.orbital.workspace.Workspace;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/workspace")
public class WorkspaceController implements Controller {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public WorkspaceController(UserRepository userRepository, TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createWorkspace(
            @RequestBody CreateWorkspaceRequest request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        var name = request.name.trim();
        if (name.isBlank()) return badRequest("Workspace name cannot be blank");
        if (user.get().workspaces().size() >= 16) return badRequest("Max workspace size reached! | Max 16 workspaces per user");
        var teamID = request.teamID;
        var workspace = new Workspace(UUID.randomUUID(), name);
        if (teamID != null) {
            var team = teamRepository.byID(teamID);
            if (team.isEmpty()) return badRequest("Team not found");
            if (!team.get().isTeamMember(user.get())) return badRequest("You are not a member of this team");
            team.get().addWorkspace(workspace);
            teamRepository.saveToDB(team.get());
            return ok("Team Workspace created!");
        }
        user.get().addWorkspace(workspace);
        return ok("User Workspace created!");
    }

    @GetMapping("/{id}/createEvent")
    public ResponseEntity<String> createEvent(
            @PathVariable String id
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        UUID workspaceId;
        try {
            workspaceId = UUID.fromString(id);
        } catch (Exception e) {
            return badRequest("Invalid workspace ID");
        }
        var workspaceOpt = user.get().workspaceByID(workspaceId, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest("Workspace not found");
        var workspace = workspaceOpt.get();
        workspace.createEvent();
        userRepository.saveToDB(user.get());
        return ok("Created");
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<Events> getEvents(@PathVariable String id) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        UUID workspaceId;
        try {
            workspaceId = UUID.fromString(id);
        } catch (Exception e) {
            return badRequest();
        }
        var events = user.get().workspaceByID(workspaceId, userRepository, teamRepository).map(Workspace::getEvents).orElse(null);
        if (events == null) return ResponseEntity.ok(new Events(new HashMap<>()));
        return ResponseEntity.ok(new Events(events));
    }

    @PostMapping("/{id}/updateEvent")
    public ResponseEntity<String> updateEvent(
            @PathVariable String id,
            @RequestBody UpdateEvent request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        UUID workspaceId;
        try {
            workspaceId = UUID.fromString(id);
        } catch (Exception e) {
            return badRequest("Invalid workspace ID");
        }
        var workspaceOpt = user.get().workspaceByID(workspaceId, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest("Workspace not found");
        var workspace = workspaceOpt.get();
        var event = new DateEvent(request.id, request.title, request.description, request.setDate, request.dateDue, request.attendees, true);
        workspace.updateEvent(event);
        userRepository.saveToDB(user.get());
        user.get().getActiveTeams(teamRepository).forEach(teamRepository::saveToDB);
        return ok("Updated");
    }

    public record UpdateEvent(
        int id,
        String title,
        String description,
        @Nullable Long setDate,
        @Nullable Long dateDue,
        Set<Long> attendees
    ) {
    }

    public record Events(
            HashMap<Integer, DateEvent> events
    ) {
    }

    public record CreateWorkspaceRequest(
            String name,
            @Nullable Long teamID
    ) { }
}
