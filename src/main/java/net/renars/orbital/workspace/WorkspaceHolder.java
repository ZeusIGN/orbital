package net.renars.orbital.workspace;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WorkspaceHolder {
    Set<Workspace> combinedWorkspaces(UserRepository userRepository, TeamRepository teamRepository);

    Set<Workspace> workspaces();

    default void serializeWorkspaces(DataHolder dataHolder) {
        var local = new DataHolder();
        var workspaces = new HashSet<DataHolder>();
        workspaces().forEach((workspace) -> workspaces.add(workspace.serialize()));
        int i = 0;
        for (var workspace : workspaces) {
            local.putCompound(i + "", workspace);
            i++;
        }
        dataHolder.putCompound("workspaces", local);
    }

    default Set<Workspace> deserializeWorkspaces(DataHolder holder) {
        var workspaces = new HashSet<Workspace>();
        if (!holder.containsKey("workspaces")) return workspaces;
        var workspacesHolder = holder.getCompound("workspaces");
        for (var entry : workspacesHolder.toMap().entrySet()) {
            var workspaceHolder = entry.getValue().m();
            workspaces.add(Workspace.deserialize(new DataHolder(workspaceHolder)));
        }
        return workspaces;
    }

    default Optional<Workspace> workspaceByID(UUID id, UserRepository userRepository, TeamRepository teamRepository) {
        return combinedWorkspaces(userRepository, teamRepository).stream()
                .filter(workspace -> workspace.getId().equals(id))
                .findFirst();
    }
}
