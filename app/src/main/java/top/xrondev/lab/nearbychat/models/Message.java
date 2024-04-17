package top.xrondev.lab.nearbychat.models;

import com.google.android.gms.nearby.connection.Payload;

public class Message {
    private final String sender;
    private final Payload payload;
    private final MessageType type;

    public Message(String sender, Payload payload, MessageType type) {
        this.sender = sender;
        this.payload = payload;
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public Payload getContent() {
        return payload;
    }

    public MessageType getType() {
        return type;
    }

    public boolean isFromMe() {
        // is send by me or from others
        return sender.equals("me");
    }
}
