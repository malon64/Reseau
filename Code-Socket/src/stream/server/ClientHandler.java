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
        this.dis =dis;
        this.dos = dos;

    }

    public void run() {
        try {
            username = dis.readUTF();
            client = new Client(username);
            System.out.println("Username : " + username);
            String line;
            while (true) {

                line = dis.readUTF();
                System.out.println("Received : " + line);

                //The user wants to add a new conversation
                if (line.charAt(0) == '+') {
                    String content = line.substring(1);
                    String [] arguments = content.split(";");
                    String convName = arguments[0];
                    Conversation conv = new Conversation(convName);
                    for (int i = 1; i < arguments.length; i++) {
                        String member = arguments[i];
                        if (!member.isEmpty()) conv.addMember(new Client(member));
                    }
                    EchoServer.conversations.add(conv);

                //The user wants to see the conversations he is in
                } else if (line.charAt(0) == '?') {

                    for (Conversation conv : EchoServer.conversations) {
                        boolean isPresent = false;
                        for (Client cl : conv.getMembers()) {
                            if (cl.getUsername().equals(client.getUsername())) isPresent = true;
                        }
                        if (isPresent) dos.writeUTF("-" + conv.getName());
                    }

                } else if (line.equals("logout")) {    //The user wants to logout
                    this.isloggedin=false;
                    this.clientSocket.close();
                    break;

                } else {
                    // break the string into message and recipient part
                    StringTokenizer st = new StringTokenizer(line, "#");
                    String content = st.nextToken();
                    String recipient = st.nextToken();
                    Message message = new Message(client, content);

                    Conversation conversation = EchoServer.findConversationByName(recipient);
                    for (Client client : conversation.getMembers()) {
                        ClientHandler clientHandler = EchoServer.findClientHandler(client);
                        if (clientHandler != null && clientHandler.isloggedin) {
                            clientHandler.dos.writeUTF( conversation.getName() + ": " + this.username + " > " + message.getContent());
                        }
                    }
                }
            }
            this.dis.close();
            this.dos.close();
        } catch (Exception e) {
            System.err.println("Error in EchoServer:" + e);
            e.printStackTrace();
        }
    }

}


