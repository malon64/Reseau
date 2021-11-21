/***
 * ClientThread
 * Example of a TCP server
 * Date: 14/12/08
 * Authors:
 */

package stream.server;

import models.Client;
import models.Conversation;
import models.Message;

import javax.tools.JavaFileManager;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.StringTokenizer;

public class ClientHandler implements Runnable {

    Scanner scn = new Scanner(System.in);
    private Socket clientSocket;

    public String getUsername() {
        return username;
    }

    private String username;
    private boolean isloggedin;

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    private Client client;
    final DataInputStream dis;
    final DataOutputStream dos;

    ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos) {
        this.clientSocket = s;
        this.isloggedin = true;
        this.dis = dis;
        this.dos = dos;

    }

    public void run() {
        try {
            username = dis.readUTF();
            client = new Client(username);
            System.out.println("Username : " + username);
            String line;
            while (true) {


                dos.writeUTF("|  MENU    |");
                dos.writeUTF("| Options:                 |");
                dos.writeUTF("1. Create a conversation");
                dos.writeUTF("2. Join a conversation, to exit write -Exit");
                dos.writeUTF("3. See my conversations");
                dos.writeUTF("4. Logout");


                String choice = dis.readUTF();
                System.out.println("Received : " + choice);



                //The user wants to add a new conversation
                if (choice.equals("1")) {
                    dos.writeUTF("Name : ");
                    String convName = dis.readUTF();
                    dos.writeUTF("Members : ");
                    String[] arguments = dis.readUTF().split(";");
                    createConversation(convName, arguments);
                    //The user wants to see the conversations he is in
                } else if (choice.equals("3")) {

                    for (Conversation conversation : EchoServer.conversations){
                        if (conversation.findClientinConv(client) != null){
                            dos.writeUTF("-" + conversation.getName());
                        }
                    }

                } else if (choice.equals("2")) {
                    String convChoice = dis.readUTF();
                    Conversation conversation = EchoServer.findConversationByName(convChoice);
                    if (conversation.findClientinConv(client) == null){
                        conversation.addMember(client);
                    }
                    while (true) {
                        String content = dis.readUTF();
                        if (content.equals("exit")) {
                            break;
                        }
                        Message message = new Message(client, content);
                        sendToGroup(conversation, message);
                    }


                } else if (choice.equals("4")) {    //The user wants to logout
                    this.isloggedin = false;
                    this.clientSocket.close();
                    break;


                }
            }
            this.dis.close();
            this.dos.close();
        } catch (Exception e) {
            System.err.println("Error in EchoServer:" + e);
            e.printStackTrace();
        }
    }

    public void sendToGroup(Conversation conversation, Message message) throws IOException {
        conversation.addMessage(message);
        for (Client client : conversation.getMembers()) {
            ClientHandler clientHandler = EchoServer.findClientHandler(client);
            if (clientHandler != null && clientHandler.isloggedin) {
                clientHandler.dos.writeUTF(conversation.getName() + ": " + this.username + " > " + message.getContent());
            }
        }
    }

    public void createConversation(String convName, String[] arguments){
        Conversation conv = new Conversation(convName);
        for (int i = 0; i < arguments.length; i++) {
            String member = arguments[i];
            if (!member.isEmpty()) conv.addMember(new Client(member));
        }
        EchoServer.conversations.add(conv);
        {
            for (Conversation convo : EchoServer.conversations){
                System.out.println(convo.getName());
                for (Client c : convo.getMembers()){
                    System.out.println(c.getUsername());
                }

            }
        }

    }




}


