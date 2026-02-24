package net.renars.orbital.user;

import net.renars.orbital.data.DataHolder;
import net.renars.orbital.utils.Serializable;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;
import java.util.Set;

public class Notifications implements Serializable {
    private final User user;
    private final Set<Message> messages = new HashSet<>();

    public Notifications(User user) {
        this.user = user;
    }

    public void addMessage(String header, String content, String href) {
        messages.add(new Message(header, content, href));
    }

    public ResponseEntity<Set<Message>> toClient() {
        return ResponseEntity.ok(messages);
    }

    @Override
    public DataHolder serialize() {
        var holder = new DataHolder();
        int i = 0;
        for (var message : messages) {
            var messageHolder = new DataHolder();
            messageHolder.putString("header", message.header);
            messageHolder.putString("content", message.content);
            messageHolder.putString("href", message.href);
            holder.putCompound(i + "", messageHolder);
            i++;
        }
        holder.putNumber("size", messages.size());
        holder.putCompound("messages", holder);
        return holder;
    }

    public void deserialize(DataHolder data) {
        var messagesHolder = data.getCompound("messages");
        int size = data.getInteger("size");
        for (int i = 0; i < size; i++) {
            var messageHolder = messagesHolder.getCompound(i + "");
            var header = messageHolder.getString("header");
            var content = messageHolder.getString("content");
            var href = messageHolder.getString("href");
            messages.add(new Message(header, content, href));
        }
    }

    public record Message(
            String header,
            String content,
            String href
    ) {

    }
}
