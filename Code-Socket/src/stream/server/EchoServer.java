/***
 * EchoServer
 * Example of a TCP server
 * Date: 10/01/04
 * Authors:
 */

package stream.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.*;

public class EchoServer {


	static Vector<ClientHandler> connectedClients = new Vector<>();
   public static void main(String args[]){
	ServerSocket listenSocket;

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
  }

  
