package models;



import java.util.Vector;
import stream.server.ClientHandler;

public class Conversation {
    private String name;
    private Vector<ClientHandler> members;

    public Conversation(String name) {
        this.name = name;
    }

    public Conversation(String name, Vector<ClientHandler> members) {
        this.name = name;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vector<ClientHandler> getMembers() {
        return members;
    }

    public void setMembers(Vector<ClientHandler> members) {
        this.members = members;
    }

    public void addMember(ClientHandler newMember){
        this.members.add(newMember);
    }

    public void deleteMember(ClientHandler member){
        this.members.remove(member);
    }
}
