package net.renars.orbital.workspace;

import lombok.Getter;
import net.renars.orbital.data.DataHolder;
import net.renars.orbital.services.TeamRepository;
import net.renars.orbital.services.UserRepository;
import net.renars.orbital.user.User;
import net.renars.orbital.utils.Serializable;

import java.util.*;

/**
 * Datu glabātuve un organizācija, kas satur kalendāra notikumus un citas lietas --Renars
 */
public abstract class Workspace implements Serializable {
    @Getter
    private final UUID id;
    @Getter
    private String name;
    private final HashMap<Integer, DateEvent> events = new HashMap<>();
    private final HashMap<Integer, DateEvent.Label> label = new HashMap<>();

    public Workspace(String id, String name) {
        this.id = UUID.fromString(id);
        this.name = name;
    }

    public Workspace(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean canAccess(User user) {
        return true;
    }

    public int nextEventID() {
        return events.size() + 1;
    }

    public void addEvent(DateEvent event) {
        events.put(event.id(), event);
    }

    public void createEvent() {
        var event = new DateEvent(nextEventID(), "New Event", "", null, null, new HashSet<>(), null, true);
        addEvent(event);
    }

    public Set<DateEvent.Label> getLabels() {
        return new HashSet<>(label.values());
    }

    public DateEvent.Label getLabel(int id) {
        return label.get(id);
    }

    public void addLabel(DateEvent.Label label) {
        var id = getHighestLabelID() + 1;
        this.label.put(id, label.withId(id));
    }

    public void addRawLabel(int id, DateEvent.Label label) {
        this.label.put(id, label);
    }

    public int getHighestLabelID() {
        return label.keySet().stream().mapToInt((e) -> e).max().orElse(0);
    }

    public void removeLabel(int id) {
        label.remove(id);
    }

    public void updateEvent(DateEvent event) {
        events.put(event.id(), event);
    }

    public HashMap<Integer, DateEvent> getEvents() {
        return new HashMap<>(events);
    }

    public HashMap<Integer, DateEvent> getEvents(int month, int year) {
        return events.entrySet().stream().filter(entry -> {
            var event = entry.getValue();
            if (event.setDate() == null) return true;
            var date = new Date(event.setDate().longValue());
            return date.getMonth() == month && date.getYear() + 1900 == year;
        }).collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
    }

    @Override
    public DataHolder serialize() {
        var holder = new DataHolder();
        holder.putString("id", id.toString());
        holder.putString("name", name);
        var eventsHolder = new DataHolder();
        for (var entry : events.entrySet()) {
            eventsHolder.putCompound(entry.getKey() + "", entry.getValue().serialize());
        }
        var labelsHolder = new DataHolder();
        for (var entry : label.entrySet()) {
            labelsHolder.putCompound(entry.getKey().hashCode() + "", entry.getValue().serialize());
        }
        holder.putCompound("labels", labelsHolder);
        holder.putCompound("events", eventsHolder);
        return holder;
    }

    public void deserializeExtra(DataHolder holder) {

    }

    public abstract void save(TeamRepository teamRepository, UserRepository userRepository);
}
