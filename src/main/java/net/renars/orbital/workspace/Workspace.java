package net.renars.orbital.workspace;

import lombok.Getter;
import net.renars.orbital.Orbital;
import net.renars.orbital.data.DataHolder;
import net.renars.orbital.utils.Serializable;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * Datu glabātuve un organizācija, kas satur kalendāra notikumus un citas lietas --Renars
 */
public class Workspace implements Serializable {
    @Getter
    private final UUID id;
    @Getter
    private String name;
    private HashMap<Integer, DateEvent> events = new HashMap<>();

    public Workspace(String id, String name) {
        this.id = UUID.fromString(id);
        this.name = name;
    }

    public Workspace(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public int nextEventID() {
        return events.size() + 1;
    }

    public void addEvent(DateEvent event) {
        events.put(event.id(), event);
    }

    public void createEvent() {
        var event = new DateEvent(nextEventID(), "New Event", "", null, null, new HashSet<>(), true);
        addEvent(event);
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
        holder.putCompound("events", eventsHolder);
        return holder;
    }

    public static Workspace deserialize(DataHolder data) {
        var id = data.getString("id");
        var name = data.getString("name");
        var workspace = new Workspace(id, name);
        var eventsHolder = data.getCompound("events");
        for (var entry : eventsHolder.toMap().entrySet()) {
            var eventData = DataHolder.from(entry.getValue().m());
            var event = DateEvent.deserialize(eventData);
            workspace.addEvent(event);
        }
        return workspace;
    }
}
