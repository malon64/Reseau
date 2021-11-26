/***
 * EchoServer
 * Example of a TCP server
 * Date: 10/01/04
 * Authors:
 */

package stream.server;

import models.Client;
import models.Conversation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.*;

/**
 * EchoServer class to listen to connecting clients and loads conversations from stored files
 */
public class EchoServer {


    static Vector<ClientHandler> connectedClients = new Vector<>();
    static Vector<Conversation> conversations = new Vector<>();

    /**
     * Main class
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String args[]) throws FileNotFoundException {
        ServerSocket listenSocket;
        loadConversations();
        try {
            listenSocket = new ServerSocket(1234); //port
            System.out.println("Server ready...");
            while (true) {
                Socket clientSocket = listenSocket.accept();
                System.out.println("Connexion from:" + clientSocket.getInetAddress());

                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                ClientHandler ch = new ClientHandler(clientSocket, dis, dos);
                Thread t = new Thread(ch);
                connectedClients.add(ch);
                t.start();
            }
        } catch (Exception e) {
            System.err.println("Error in EchoServer:" + e);
        }
    }

    /**
     * Returns the ClientHandler object with a given client (in connectedClients)
     * @param client
     * @return
     */
    public static ClientHandler findClientHandler(Client client) {
        for (ClientHandler ch : connectedClients) {
            if (ch.getUsername().equals(client.getUsername())) {
                return ch;
            }
        }
        return null;
    }

    /**
     * Returns the Client object based on its name
     * @param name
     * @return
     */
    public static Client findClientByName(String name){
        for (ClientHandler ch : connectedClients) {
            if (ch.getUsername().equals(name)) {
                return ch.getClient();
            }
        }
        return null;
    }

    /**
     * Returns a Conversation object using a given name
     * @param name
     * @return
     */
    public static Conversation findConversationByName(String name) {
        for (Conversation conv : conversations) {
            if (conv.getName().equals(name)) {
                return conv;
            }
        }
        return null;
    }


    /**
     * A method that loads a conversation from existing stored files
     * @throws FileNotFoundException
     */
    public static void loadConversations() throws FileNotFoundException {
        File dir = new File("files");
        String[] pathNames = dir.list();
        if (pathNames != null){
            for (String fileName : pathNames) {
                if (findConversationByName(fileName) == null) {
                    Conversation conv = new Conversation(fileName);
                    File file = new File("files/" + fileName);
                    Scanner reader = new Scanner(file);
                    String data = reader.nextLine();
                    int ind = data.indexOf(":") + 1;
                    if (ind != -1) {
                        String[] members = data.substring(ind).split(";");
                        for (String member : members) {
                            Client client = new Client(member);
                            if (EchoServer.findClientHandler(client) == null) {
                                conv.addMember(client);
                            }
                        }
                    }
                    conversations.add(conv);
                }
            }
        }
    }
}






  
