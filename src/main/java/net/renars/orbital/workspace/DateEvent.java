package net.renars.orbital.workspace;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.user.User;
import net.renars.orbital.utils.Serializable;

import java.util.HashSet;
import java.util.Set;

public record DateEvent(
        int id,
        String title,
        String description,
        Number setDate,
        Number dateDue,
        Set<Long> attendees,
        String label, // var but "" --Renars
        boolean editable
) implements Serializable {
    public boolean isVisibleTo(User user) {
        return attendees.isEmpty() || attendees.contains(user.id());
    }

    @Override
    public DataHolder serialize() {
        var holder = new DataHolder();
        holder.putNumber("id", id);
        holder.putString("title", title);
        holder.putString("description", description);
        if (setDate != null) holder.putNumber("setDate", setDate);
        if (dateDue != null) holder.putNumber("dueDate", dateDue);
        var attendeesHolder = new DataHolder();
        int i = 0;
        for (var attendee : attendees) {
            attendeesHolder.putLong(i + "", attendee);
            i++;
        }
        holder.putCompound("attendees", attendeesHolder);
        holder.putString("label", label);
        holder.putBoolean("editable", editable);
        return holder;
    }

    public static DateEvent deserialize(DataHolder data) {
        var id = data.getInteger("id");
        var title = data.getString("title");
        var description = data.getString("description");
        var setDate = data.containsKey("setDate") ? data.getLong("setDate") : null;
        var dueDate = data.containsKey("dueDate") ? data.getLong("dueDate") : null;
        var attendeesHolder = data.getCompound("attendees");
        var label = data.containsKey("label") ? data.getString("label") : "";
        var attendees = new HashSet<Long>();
        for (var key : attendeesHolder.toMap().keySet()) {
            attendees.add(attendeesHolder.getLong(key));
        }
        var editable = data.getBoolean("editable");
        return new DateEvent(
                id,
                title,
                description,
                setDate,
                dueDate,
                attendees,
                label,
                editable
        );
    }

    public record Label(
            int id,
            String name,
            String color
    ) implements Serializable {
        public Label withId(int id) {
            return new Label(id, name, color);
        }

        @Override
        public DataHolder serialize() {
            var holder = new DataHolder();
            holder.putNumber("id", id);
            holder.putString("name", name);
            holder.putString("color", color);
            return holder;
        }

        public static Label deserialize(DataHolder data) {
            var id = data.getInteger("id", -1);
            var name = data.getString("name");
            var color = data.getString("color");
            return new Label(id, name, color);
        }
    }
}
