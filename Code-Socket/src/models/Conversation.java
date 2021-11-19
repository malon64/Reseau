package models;



import java.util.Vector;
import stream.server.ClientHandler;

public class Conversation {
    private String name;
    private Vector<Client> members;

    public Conversation(String name) {
        this.name = name;
        this.members = new Vector<>();
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

    public void addMember(Client newMember){
        this.members.add(newMember);
    }

    public void deleteMember(Client member){
        this.members.remove(member);
    }
}
