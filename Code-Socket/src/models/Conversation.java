package models;



import java.util.Vector;
import stream.server.ClientHandler;

public class Room {
    private String name;
    private Vector<ClientHandler> members;

    public Room(String name) {
        this.name = name;
    }

    public Room(String name, Vector<ClientHandler> members) {
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

    public void addMember()
}
