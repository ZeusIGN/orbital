package net.renars.orbital.api;

import jakarta.annotation.Nullable;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.team.OPermissions;
import net.renars.orbital.workspace.DateEvent;
import net.renars.orbital.workspace.TeamWorkspace;
import net.renars.orbital.workspace.UserWorkspace;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Workspace (publiskais) API.
 * --Renars
 */
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
        // arbitrary limits, lai neviens neizveido 9999 workspaces un neapgrūtina serveri --Renars
        if (user.get().workspaces().size() >= 16)
            return badRequest("Max workspace size reached! | Max 16 workspaces per user");
        var teamID = request.teamID;
        if (teamID != null) {
            var team = teamRepository.byID(teamID);
            if (team.isEmpty()) return badRequest("Team not found");
            if (!team.get().isTeamMember(user.get())) return badRequest("You are not a member of this team");
            team.get().addWorkspace(new TeamWorkspace(UUID.randomUUID().toString(), name, team.get().id()));
            teamRepository.saveToDB(team.get());
            return ok("Team Workspace created!");
        }
        user.get().addWorkspace(new UserWorkspace(UUID.randomUUID().toString(), name, user.get().id()));
        userRepository.saveToDB(user.get());
        return ok("User Workspace created!");
    }

    @PostMapping("/{id}/edit")
    public ResponseEntity<String> addRoleToWorkspace(
            @PathVariable String id,
            @RequestBody RoleRequest request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest("Workspace not found");
        var workspace = workspaceOpt.get();
        if (!(workspace instanceof TeamWorkspace teamWorkspace)) return badRequest("Only team workspaces can be edited");
        var team = teamWorkspace.getTeam(teamRepository);
        if (!workspace.canAccess(user.get())) return badRequest("You don't have access to this workspace");
        if (!team.hasPermission(user.get(), OPermissions.EDIT_WORKSPACES)) return badRequest("You do not have permission to edit this workspace");
        teamWorkspace.addAllowedRole(request.roleName);
        return ok("Workspace updated!");
    }

    @DeleteMapping("/{id}/edit")
    public ResponseEntity<String> removeRoleFromWorkspace(
            @PathVariable String id,
            @RequestBody RoleRequest request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest("Workspace not found");
        var workspace = workspaceOpt.get();
        if (!(workspace instanceof TeamWorkspace teamWorkspace)) return badRequest("Only team workspaces can be edited");
        var team = teamWorkspace.getTeam(teamRepository);
        if (!workspace.canAccess(user.get())) return badRequest("You don't have access to this workspace");
        if (!team.hasPermission(user.get(), OPermissions.EDIT_WORKSPACES)) return badRequest("You do not have permission to edit this workspace");
        teamWorkspace.removeAllowedRole(request.roleName);
        return ok("Workspace updated!");
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<Collection<String>> getWorkspaceRoles(
            @PathVariable String id
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest();
        var workspace = workspaceOpt.get();
        if (!(workspace instanceof TeamWorkspace teamWorkspace)) return badRequest();
        if (!workspace.canAccess(user.get())) return badRequest();
        return ok(teamWorkspace.getAllowedRoles());
    }

    @GetMapping("/{id}/createEvent")
    public ResponseEntity<String> createEvent(
            @PathVariable String id
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest("Unauthorized");
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest("Workspace not found");
        var workspace = workspaceOpt.get();
        if (!workspace.canAccess(user.get())) return badRequest();
        workspace.createEvent();
        workspace.save(teamRepository, userRepository);
        return ok("Created");
    }

    @PostMapping("/{id}/events")
    public ResponseEntity<Events> getEvents(
            @PathVariable String id,
            @RequestBody RequestEvents request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest();
        var workspace = workspaceOpt.get();
        if (!workspace.canAccess(user.get())) return badRequest();
        var events = workspaceOpt.map((w) -> w.getEvents(request.month, request.year))
                .orElse(null);
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
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest("Workspace not found");
        var workspace = workspaceOpt.get();
        if (!workspace.canAccess(user.get())) return badRequest("You do not have permission to edit this workspace");
        var event = new DateEvent(
                request.id,
                request.title,
                request.description,
                request.setDate,
                request.dateDue,
                request.attendees,
                request.label,
                true
        );
        workspace.updateEvent(event);
        workspace.save(teamRepository, userRepository);
        return ok("Updated");
    }

    @GetMapping("/{id}/labels")
    public ResponseEntity<Set<DateEvent.Label>> getLabels(
            @PathVariable String id
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest();
        var workspace = workspaceOpt.get();
        if (!workspace.canAccess(user.get())) return badRequest();
        return ResponseEntity.ok(workspace.getLabels());
    }

    @PostMapping("/{id}/labels")
    public ResponseEntity<String> addLabel(
            @PathVariable String id,
            @RequestBody DateEvent.Label label
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest();
        var workspace = workspaceOpt.get();
        if (!workspace.canAccess(user.get())) return badRequest();
        workspace.addLabel(label);
        workspace.save(teamRepository, userRepository);
        return ok("Label added");
    }

    @DeleteMapping("/{id}/labels")
    public ResponseEntity<String> removeLabel(
            @PathVariable String id,
            @RequestBody int labelID
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest();
        var workspace = workspaceOpt.get();
        if (!workspace.canAccess(user.get())) return badRequest();
        workspace.removeLabel(labelID);
        workspace.save(teamRepository, userRepository);
        return ok("Label removed");
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<WorkspaceInfo> info(
            @PathVariable String id
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var workspaceOpt = user.get().workspaceByID(id, userRepository, teamRepository);
        if (workspaceOpt.isEmpty()) return badRequest();
        var workspace = workspaceOpt.get();
        if (!workspace.canAccess(user.get())) return badRequest();
        var teamId = -1L;
        var canEdit = false;
        if (workspace instanceof TeamWorkspace teamWorkspace) {
            teamId = teamWorkspace.teamID;
            var team = teamWorkspace.getTeam(teamRepository);
            canEdit = teamWorkspace.canAccess(user.get()) && team.hasPermission(user.get(), OPermissions.EDIT_WORKSPACES);
        }
        return ResponseEntity.ok(new WorkspaceInfo(
                workspace.getName(),
                teamId,
                canEdit
        ));
    }

    public record UpdateEvent(
            int id,
            String title,
            String description,
            @Nullable Long setDate,
            @Nullable Long dateDue,
            Set<Long> attendees,
            String label
    ) {
    }

    public record Events(
            HashMap<Integer, DateEvent> events
    ) {
    }

    public record CreateWorkspaceRequest(
            String name,
            @Nullable Long teamID
    ) {
    }

    public record RoleRequest(
            String roleName
    ) {
    }

    public record WorkspaceInfo(
            String name,
            long teamId,
            boolean canEdit
    ) {
    }

    public record RequestEvents(
            int month,
            int year
    ) {
    }
}
