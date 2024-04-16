package top.xrondev.lab.nearbychat.models;

public class Message {
    private final String sender;
    private final String content;
    private final MessageType type;

    public Message(String sender, String content, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public MessageType getType() {
        return type;
    }

    public boolean isFromMe() {
        // is send by me or from others
        return sender.equals("me");
    }
}
