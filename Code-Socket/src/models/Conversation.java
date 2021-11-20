package models;


import java.lang.reflect.Member;
import java.util.Vector;

import stream.server.ClientHandler;

public class Conversation {
    private String name;
    private Vector<Client> members;
    private Vector<Message> messages;


    public Conversation(String name) {
        this.name = name;
        this.members = new Vector<>();
        this.messages = new Vector<>();
    }

    public Conversation(String name, Vector<Client> members) {
        this.name = name;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vector<Client> getMembers() {
        return members;
    }

    public void setMembers(Vector<Client> members) {
        this.members = members;
    }

    public void addMember(Client newMember) {
        this.members.add(newMember);
    }

    public void deleteMember(Client member) {
        this.members.remove(member);
    }

    public Client findClientinConv(Client member) {
        for (Client client : members) {
            if (client.getUsername().equals(member.getUsername())) {
                return client;
            }
        }
        return null;
    }

    public Vector<Message> getMessages() {
        return messages;
    }

    public void setMessages(Vector<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message){
        this.messages.add(message);
    }
}
