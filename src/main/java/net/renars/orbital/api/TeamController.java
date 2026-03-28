package net.renars.orbital.api;

import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.team.OPermissions;
import net.renars.orbital.team.Permissions;
import net.renars.orbital.team.Team;
import net.renars.orbital.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
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
        if (teamRepository.fromUser(user.get()).size() >= 16)
            return badRequest("Max team size reached! | Max 16 teams per user");
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
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest("Team not found");
        var team = teamOpt.get();
        if (!team.isTeamMember(user.get())) return badRequest("You are not a member of this team");
        if (!team.hasPermission(user.get(), OPermissions.MANAGE_MEMBERS))
            return badRequest("You do not have permission to invite teammates to this team!");
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
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        return ResponseEntity.ok(new TeamInfo(team.id(), team.getName()));
    }

    @PutMapping("/{teamID}")
    public ResponseEntity<String> modifyTeamInfo(
            @PathVariable long teamID,
            @RequestBody RegisterTeamRequest request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.hasPermission(user.get(), OPermissions.MANAGE_TEAM))
            return badRequest("You do not have permission to modify this team!");
        var teamName = request.name.trim();
        if (teamName.isBlank()) return badRequest("Team name cannot be blank");
        team.setName(teamName);
        teamRepository.saveToDB(team);
        return ok("Team info modified!");
    }

    @DeleteMapping("/{teamID}")
    public ResponseEntity<String> deleteTeam(
            @PathVariable long teamID
    ) {
        //var user = getAuthUser();
        //if (user.isEmpty()) return badRequest();
        //var teamOpt = getTeam(user, teamID);
        //if (teamOpt.isEmpty()) return badRequest();
        //var team = teamOpt.get();
        //if (!team.isOwner(user.get()))
        //    return badRequest("You do not have permission to delete this team!");
        //teamRepository.delete(team.id());
        return ok("a");
    }

    @PostMapping("/{teamID}/updateRole/{username}")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable long teamID,
            @PathVariable String username,
            @RequestBody ModifyMemberRoleRequest request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.hasPermission(user.get(), OPermissions.MANAGE_PERMISSIONS))
            return badRequest("You do not have permission to modify members of this team!");
        var memberOpt = userRepository.byUsername(username);
        if (memberOpt.isEmpty()) return badRequest("User not found");
        var member = memberOpt.get();
        if (!team.isTeamMember(member)) return badRequest("User is not a member of this team");
        if (team.getRoleOf(user.get()).priority() <= team.getRole(request.role()).priority())
            return badRequest("You cannot assign a role that is higher or equal to your current role!");
        team.setRole(member, request.role());
        teamRepository.saveToDB(team);
        return ok("Member updated!");
    }

    @PostMapping("/{teamID}/updatePermissions/{username}")
    public ResponseEntity<String> updateMemberPermissions(
            @PathVariable long teamID,
            @PathVariable String username,
            @RequestBody ModifyMemberPermissionsRequest request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.hasPermission(user.get(), OPermissions.MANAGE_PERMISSIONS))
            return badRequest("You do not have permission to modify members of this team!");
        var memberOpt = userRepository.byUsername(username);
        if (memberOpt.isEmpty()) return badRequest("User not found");
        var member = memberOpt.get();
        if (!team.isTeamMember(member)) return badRequest("User is not a member of this team");
        var base = team.getMember(member).permissions();
        var permissions = base == null ? new Permissions() : base;
        team.setPermissions(member, permissions.modifyPermissions(request.permissions));
        teamRepository.saveToDB(team);
        return ok("Member permissions updated!");
    }

    @GetMapping("/{teamID}/resetPermissions/{username}")
    public ResponseEntity<String> resetMemberPermissions(
            @PathVariable long teamID,
            @PathVariable String username
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.hasPermission(user.get(), OPermissions.MANAGE_PERMISSIONS))
            return badRequest("You do not have permission to modify members of this team!");
        var memberOpt = userRepository.byUsername(username);
        if (memberOpt.isEmpty()) return badRequest("User not found");
        var member = memberOpt.get();
        if (!team.isTeamMember(member)) return badRequest("User is not a member of this team");
        team.setPermissions(member, null);
        teamRepository.saveToDB(team);
        return ok("Member permissions reset!");
    }

    @GetMapping("/{teamID}/members")
    public ResponseEntity<Collection<ResultMember>> getTeamMembers(
            @PathVariable long teamID
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        return ResponseEntity.ok(
                team.members()
                        .stream()
                        .map(member -> new ResultMember(
                                member.username(),
                                member.role(),
                                member.permissions(team).toMap(),
                                member.additionalInfo()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/{teamID}/join")
    public ResponseEntity<String> joinTeam(
            @PathVariable long teamID
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = teamRepository.byID(teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.isInvited(user.get())) return badRequest("You are not invited to this team");
        team.addMember(user.get(), team.getRole("default"));
        userRepository.saveToDB(user.get());
        teamRepository.saveToDB(team);
        return ok("Joined team!");
    }

    @GetMapping("/{teamID}/roles")
    public ResponseEntity<Collection<ResultRole>> getTeamRoles(
            @PathVariable long teamID
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        return ResponseEntity.ok(team.getRoles()
                .stream()
                .map(role -> new ResultRole(role.name(), role.permissions().toMap(), role.priority()))
                .collect(Collectors.toList())
        );
    }

    @PostMapping("/{teamID}/roles")
    public ResponseEntity<String> createRole(
            @PathVariable long teamID,
            @RequestBody ResultRole request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.hasPermission(user.get(), OPermissions.MANAGE_PERMISSIONS))
            return badRequest("You do not have permission to create roles!");
        if (team.roleExists(request.name)) return badRequest("Role already exists");
        var added = team.addRole(user.get(), request.name, new Permissions(request.permissions));
        teamRepository.saveToDB(team);
        return added ? ok("Role created!") : badRequest("Duplicate role name!");
    }

    @PutMapping("/{teamID}/roles/{roleName}")
    public ResponseEntity<String> modifyRole(
            @PathVariable long teamID,
            @PathVariable String roleName,
            @RequestBody ModifyRoleRequest request
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.hasPermission(user.get(), OPermissions.MANAGE_PERMISSIONS))
            return badRequest("You do not have permission to modify roles!");
        if (!team.roleExists(roleName)) return badRequest("Role not found");
        var res = team.modifyRolePermissions(user.get(), roleName, request.permissions());
        teamRepository.saveToDB(team);
        return res.isError() ? badRequest(res.errorMsg()) : ok(res.value());
    }

    @DeleteMapping("/{teamID}/roles/{roleName}")
    public ResponseEntity<String> deleteRole(
            @PathVariable long teamID,
            @PathVariable String roleName
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.hasPermission(user.get(), OPermissions.MANAGE_PERMISSIONS))
            return badRequest("You do not have permission to delete roles!");
        if (!team.roleExists(roleName)) return badRequest("Role not found");
        if (roleName.equals("default")) return badRequest("Cannot delete default role");
        if (roleName.equals(team.getRoleOf(user.get()).name()))
            return badRequest("You cannot delete a role that you are currently assigned to!");
        team.removeRole(roleName);
        teamRepository.saveToDB(team);
        return ok("Role deleted!");
    }

    @PostMapping("/{teamID}/roles/{roleName}/priority")
    public ResponseEntity<String> modifyRolePriority(
            @PathVariable long teamID,
            @PathVariable String roleName,
            @RequestBody int priority
    ) {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var teamOpt = getTeam(user, teamID);
        if (teamOpt.isEmpty()) return badRequest();
        var team = teamOpt.get();
        if (!team.isAdmin(user.get()))
            return badRequest("You do not have permission to modify priority!");
        if (!team.roleExists(roleName)) return badRequest("Role not found");
        var res = team.modifyRolePriority(user.get(), roleName, priority);
        teamRepository.saveToDB(team);
        return res.isError() ? badRequest(res.errorMsg()) : ok(res.value());
    }

    @GetMapping("/permissions")
    public ResponseEntity<Set<ResultPermission>> getPermissions() {
        var user = getAuthUser();
        if (user.isEmpty()) return badRequest();
        var perms = OPermissions.all()
                .stream()
                .map((permission) -> new ResultPermission(
                        permission.name(),
                        permission.requirements().toMap()
                ))
                .collect(Collectors.toSet());
        return ResponseEntity.ok(perms);
    }

    public Optional<Team> getTeam(Optional<User> user, long teamID) {
        if (user.isEmpty()) return Optional.empty();
        var teamOpt = teamRepository.byID(teamID);
        if (teamOpt.isEmpty()) return Optional.empty();
        var team = teamOpt.get();
        if (!team.isTeamMember(user.get())) return Optional.empty();
        return Optional.of(team);
    }

    public record TeamInfo(
            long id,
            String name
    ) {
    }

    public record RegisterTeamRequest(String name) {
    }

    public record InviteTeammate(String username) {
    }

    public record ModifyMemberRoleRequest(String role) {
    }

    public record ModifyMemberPermissionsRequest(HashMap<String, Boolean> permissions) {
    }

    public record ModifyRoleRequest(HashMap<String, Boolean> permissions) {
    }

    public record ResultPermission(
            String name,
            HashMap<String, Boolean> requirements
    ) {
    }

    public record ResultRole(
            String name,
            HashMap<String, Boolean> permissions,
            int priority
    ) {
    }

    public record ResultMember(
            String username,
            String role,
            HashMap<String, Boolean> permissions,
            Team.AdditionalInfo additionalInfo
    ) {
    }
}
