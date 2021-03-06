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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Class ClientHandler to handle the connected client's user interface and thus choices
 */
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

    /**
     * the starting and running method when a client is created
     */
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
                dos.writeUTF("2. Join a conversation, to exit write : exit");
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
                    dos.writeUTF("Name: ");
                    String convChoice = dis.readUTF();
                    Conversation conversation = EchoServer.findConversationByName(convChoice);
                    if (conversation == null){
                        dos.writeUTF("Conversation doesn't exit yet");
                    } else {
                        if (conversation.findClientinConv(client) == null){
                            conversation.addMember(client);
                            addMemberToFile(conversation.getName(), client.getUsername());
                        }
                        readMessagesFromFile(convChoice);
                        dos.writeUTF("You can start chatting!");
                        while (true) {
                            String content = dis.readUTF();
                            if (content.equals("exit")) {
                                break;
                            }
                            Message message = new Message(client, content);
                            sendToGroup(conversation, message);
                        }

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

    /**
     * This method sends a Message object to all members of the conversation in entry
     * @param conversation
     * @param message
     * @throws IOException
     */
    public void sendToGroup(Conversation conversation, Message message) throws IOException {
        conversation.addMessage(message);
        writeMessageInFile(conversation.getName(), message);
        for (Client client : conversation.getMembers()) {
            ClientHandler clientHandler = EchoServer.findClientHandler(client);
            if (clientHandler != null && clientHandler.isloggedin) {
                clientHandler.dos.writeUTF(conversation.getName() + ": " + this.username + " > " + message.getContent());
            }
        }
    }

    /**
     * This method creates a conversation with a given name and given members
     * It also creates a file related to the conversation
     * @param convName
     * @param arguments
     * @throws IOException
     */
    public void createConversation(String convName, String[] arguments) throws IOException {
        Conversation conv = new Conversation(convName);
        for (int i = 0; i < arguments.length; i++) {
            String member = arguments[i];
            if (!member.isEmpty()) conv.addMember(new Client(member));
        }
        EchoServer.conversations.add(conv);
        // file param
        File convFile = new File("files/"+conv.getName());
        System.out.println(convFile.getAbsolutePath());
        // if file doesnt exist
        if (convFile.createNewFile()){
            FileWriter output = new FileWriter(convFile);
            String convParam = conv.getName()+":";
            for (Client c : conv.getMembers()){
                convParam += c.getUsername()+";";
            }
            output.write(convParam);
            output.close();
        }
        // do nothing when file already exists
    }

    /**
     * Writes the given Message in the file related to the conversation
     * @param convName
     * @param message
     * @throws IOException
     */
    public void writeMessageInFile(String convName, Message message) throws IOException {
        FileWriter fstream = new FileWriter("files/"+convName, true);
        BufferedWriter out = new BufferedWriter(fstream);
        String sender = message.getSender().getUsername();
        String content = message.getContent();
        String line = sender+">"+content;
        out.newLine();
        out.write(line);
        out.close();
    }

    /**
     * Reads all stored messages in the file related to the conversation and loads them
     * in the Conversation object (for the application's duration of life)
     * @param convName
     * @throws IOException
     */
    public void readMessagesFromFile(String convName) throws IOException {
        File file = new File("files/" + convName);
        Scanner reader = new Scanner(file);
        // don't read first line
        reader.nextLine();
        while(reader.hasNextLine()){
            String data = reader.nextLine();
            dos.writeUTF(data);
            String senderName = data.split(">")[0];
            Client sender = EchoServer.findClientByName(senderName);
            String content = data.split(">")[1];
            Message message = new Message(sender, content);
            EchoServer.findConversationByName(convName).addMessage(message);
        }
    }

    /**
     * Writes a new member in the conversation's file config
     * @param convName
     * @param clientName
     * @throws IOException
     */
    public void addMemberToFile(String convName, String clientName) throws IOException {
        Path path = Paths.get("files/"+convName);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String data = lines.get(0);
        lines.set(0, data+clientName+";");
        Files.write(path, lines, StandardCharsets.UTF_8);
    }
}


