package models;

public class Message {

    private Client sender;
    private String content;

    public Message(Client sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    public Client getSender() {
        return sender;
    }

    public void setSender(Client sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
