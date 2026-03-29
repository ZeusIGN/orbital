package net.renars.orbital.workspace;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WorkspaceHolder<T extends Workspace> {
    // primāri Team, bet varbūt kaut kad būs arī cits veids, kas varētu saturēt workspaces, tāpēc šī metode ir šeit --Renars
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

    T create(String ID, String name);

    default Set<T> deserializeWorkspaces(DataHolder holder) {
        var workspaces = new HashSet<T>();
        if (!holder.containsKey("workspaces")) return workspaces;
        var workspacesHolder = holder.getCompound("workspaces");
        for (var entry : workspacesHolder.toMap().entrySet()) {
            var workspaceHolder = entry.getValue().m();
            workspaces.add(deserialize(new DataHolder(workspaceHolder)));
        }
        return workspaces;
    }

    default T deserialize(DataHolder data) {
        var id = data.getString("id");
        var name = data.getString("name");
        var workspace = create(id, name);
        workspace.deserializeExtra(data);
        var labelsHolder = data.containsKey("labels") ? data.getCompound("labels") : new DataHolder();
        for (var entry : labelsHolder.toMap().entrySet()) {
            var labelData = DataHolder.from(entry.getValue().m());
            var label = DateEvent.Label.deserialize(labelData);
            if (label.id() != -1) workspace.addRawLabel(label.id(), label);
            else workspace.addLabel(label);
        }
        var eventsHolder = data.getCompound("events");
        for (var entry : eventsHolder.toMap().entrySet()) {
            var eventData = DataHolder.from(entry.getValue().m());
            var event = DateEvent.deserialize(eventData);
            workspace.addEvent(event);
        }
        return workspace;
    }

    default Optional<Workspace> workspaceByID(String id, UserRepository userRepository, TeamRepository teamRepository) {
        try {
            var uuid = UUID.fromString(id);
            return workspaceByID(uuid, userRepository, teamRepository);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    default Optional<Workspace> workspaceByID(UUID id, UserRepository userRepository, TeamRepository teamRepository) {
        return combinedWorkspaces(userRepository, teamRepository).stream()
                .filter(workspace -> workspace.getId().equals(id))
                .findFirst();
    }
}
