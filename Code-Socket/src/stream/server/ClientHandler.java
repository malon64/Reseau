/***
 * ClientThread
 * Example of a TCP server
 * Date: 14/12/08
 * Authors:
 */

package stream.server;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.StringTokenizer;

public class ClientHandler implements Runnable {

    Scanner scn = new Scanner(System.in);
    private Socket clientSocket;
    private String username;
    private boolean isloggedin;
    final DataInputStream dis;
    final DataOutputStream dos;

    ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos) {
        this.clientSocket = s;
        this.isloggedin = true;
        this.dis =dis;
        this.dos = dos;

    }

    public String getUsername() {
        return username;
    }


    public void run() {
        try {
            username = dis.readUTF();
            System.out.println("Username : " + username);
            String line;
            while (true) {

                line = dis.readUTF();
                System.out.println("Received : " + line);

                if(line.equals("logout")){
                    this.isloggedin=false;
                    this.clientSocket.close();
                    break;
                }

                // break the string into message and recipient part
                StringTokenizer st = new StringTokenizer(line, "#");
                String MsgToSend = st.nextToken();
                String recipient = st.nextToken();

                for (ClientHandler ch : Server.connectedClients)
                {
                    // if the recipient is found, write on its
                    // output stream
                    if (ch.getUsername().equals(recipient) && ch.isloggedin==true)
                    {
                        ch.dos.writeUTF(this.username+" : "+MsgToSend);
                        break;
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


