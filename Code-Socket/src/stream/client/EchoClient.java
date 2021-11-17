/***
 * EchoClient
 * Example of a TCP client 
 * Date: 10/01/04
 * Authors:
 */
package stream.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;


public class EchoClient {


    /**
     * main method
     * accepts a connection, receives a message from client then sends an echo to the client
     **/
    public static void main(String[] args) throws IOException {

        Scanner scn = new Scanner(System.in);
        Socket echoSocket = null;
        PrintStream socOut = null;
        BufferedReader stdIn = null;
        BufferedReader socIn = null;

        String username = "";

        if (args.length != 1) {
            System.out.println("Usage: java EchoClient <EchoServer username>");
            System.exit(1);
        }

        try {
            // creation socket ==> connexion
            username = args[0];
            InetAddress ip = InetAddress.getByName("localhost");
            echoSocket = new Socket(ip, 1234);
            // obtaining input and out streams
            DataInputStream dis = new DataInputStream(echoSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(echoSocket.getOutputStream());

            dos.writeUTF(username);
            // sendMessage thread
            Thread sendMessage = new Thread(new Runnable()
            {
                @Override
                public void run() {
                    while (true) {

                        // read the message to deliver.
                        String msg = scn.nextLine();

                        try {
                            // write on the output stream
                            dos.writeUTF(msg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // readMessage thread
            Thread readMessage = new Thread(new Runnable()
            {
                @Override
                public void run() {

                    while (true) {
                        try {
                            // read the message sent to this client
                            String msg = dis.readUTF();
                            System.out.println(msg);
                        } catch (IOException e) {

                            e.printStackTrace();
                        }
                    }
                }
            });

            sendMessage.start();
            readMessage.start();

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host:" + args[0]);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "
                    + "the connection to:" + args[0]);
            System.exit(1);
        }
    }
}


