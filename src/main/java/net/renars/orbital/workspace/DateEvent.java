package net.renars.orbital.workspace;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.utils.Serializable;

import java.util.HashSet;
import java.util.Set;

public record DateEvent(
        int id,
        String title,
        String description,
        Number setDate,
        Number dueDate,
        Set<Long> attendees,
        boolean editable
) implements Serializable {
    @Override
    public DataHolder serialize() {
        var holder = new DataHolder();
        holder.putNumber("id", id);
        holder.putString("title", title);
        holder.putString("description", description);
        if (setDate != null) holder.putNumber("setDate", setDate);
        if (dueDate != null) holder.putNumber("dueDate", dueDate);
        var attendeesHolder = new DataHolder();
        int i = 0;
        for (var attendee : attendees) {
            attendeesHolder.putLong(i + "", attendee);
            i++;
        }
        holder.putCompound("attendees", attendeesHolder);
        holder.putBoolean("editable", editable);
        return holder;
    }

    public static DateEvent deserialize(DataHolder data) {
        var id = data.getInteger("id");
        var title = data.getString("title");
        var description = data.getString("description");
        var setDate = data.containsKey("setDate") ? data.getNumber("setDate") : null;
        var dueDate = data.containsKey("dueDate") ? data.getNumber("dueDate") : null;
        var attendeesHolder = data.getCompound("attendees");
        var attendees = new HashSet<Long>();
        for (var key : attendeesHolder.toMap().keySet()) {
            attendees.add(attendeesHolder.getLong(key));
        }
        var editable = data.getBoolean("editable");
        return new DateEvent(id, title, description, setDate, dueDate, attendees, editable);
    }
}
