package models;


import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Member;
import java.util.Scanner;
import java.util.Vector;

import stream.server.ClientHandler;
import stream.server.EchoServer;

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

    public Conversation getConvFromFile(String convName) throws FileNotFoundException {
        File dir = new File("Code-Socket/files");
        String[] pathNames = dir.list();
        for (String fileName : pathNames){
            if (fileName.equals(convName)){
                Conversation conv = new Conversation(convName);
                File file = new File("Code-Socket/files"+convName);
                Scanner reader = new Scanner(file);
                String data = reader.nextLine();
                int ind = data.indexOf(":");
                if (ind != -1){
                    String[] members = data.substring(ind).split(";");
                    for (String member : members){
                        Client client = new Client(member);
                        if (EchoServer.findClientHandler(client) != null){
                            conv.addMember(client);
                        }
                    }
                }
                return conv;
            }
        }
        return null;
    }



}
