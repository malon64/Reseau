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
                        System.out.println("conversation is" + conversation.getName() + " " + conversation.getMembers());
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
                    readMessagesFromFile(convChoice);
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
        writeMessageInFile(conversation.getName(), message);
        for (Client client : conversation.getMembers()) {
            ClientHandler clientHandler = EchoServer.findClientHandler(client);
            if (clientHandler != null && clientHandler.isloggedin) {
                clientHandler.dos.writeUTF(conversation.getName() + ": " + this.username + " > " + message.getContent());
            }
        }
    }

    public void createConversation(String convName, String[] arguments) throws IOException {
        Conversation conv = new Conversation(convName);
        for (int i = 0; i < arguments.length; i++) {
            String member = arguments[i];
            if (!member.isEmpty()) conv.addMember(new Client(member));
        }
        EchoServer.conversations.add(conv);
        // file param
        File convFile = new File("Code-Socket/files/"+conv.getName());
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

    public void writeMessageInFile(String convName, Message message) throws IOException {
        FileWriter fstream = new FileWriter("Code-Socket/files/"+convName, true);
        BufferedWriter out = new BufferedWriter(fstream);
        String sender = message.getSender().getUsername();
        String content = message.getContent();
        String line = sender+">"+content;
        out.newLine();
        out.write(line);
        out.close();
    }

    public void readMessagesFromFile(String convName) throws IOException {
        File file = new File("Code-Socket/files/" + convName);
        Scanner reader = new Scanner(file);
        // dont read first line
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




}


