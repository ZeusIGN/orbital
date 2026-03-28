package net.renars.orbital.workspace;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.team.Team;
import net.renars.orbital.user.User;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class TeamWorkspace extends Workspace {
    public final long teamID;
    private final Set<String> allowedRoles = new HashSet<>();
    private Predicate<User> accessPredicate = user -> false;

    public TeamWorkspace(
            String id,
            String name,
            long teamID
    ) {
        super(id, name);
        this.teamID = teamID;
    }

    public Team getTeam(TeamRepository teamRepository) {
        return teamRepository.byID(teamID).orElseThrow(() -> new IllegalStateException("Team with ID " + teamID + " not found!"));
    }

    public void setupPredicate(Predicate<User> predicate) {
        this.accessPredicate = predicate;
    }

    @Override
    public boolean canAccess(User user) {
        return accessPredicate.test(user);
    }

    public void setAllowedRoles(Set<String> allowedRoles) {
        this.allowedRoles.clear();
        this.allowedRoles.addAll(allowedRoles);
    }

    public void addAllowedRole(String role) {
        this.allowedRoles.add(role);
    }

    public void removeAllowedRole(String role) {
        this.allowedRoles.remove(role);
    }

    public Set<String> getAllowedRoles() {
        return Set.copyOf(allowedRoles);
    }

    public boolean isRoleAllowed(String role) {
        return allowedRoles.isEmpty() || allowedRoles.contains(role);
    }

    @Override
    public DataHolder serialize() {
        var holder = super.serialize();
        holder.putList("allowedRoles", allowedRoles.stream().toList(), (str) -> AttributeValue.builder().s(str).build());
        return holder;
    }

    @Override
    public void deserializeExtra(DataHolder holder) {
        var roles = holder.getList("allowedRoles", AttributeValue::s);
        if (roles != null) setAllowedRoles(Set.copyOf(roles));
    }
}
