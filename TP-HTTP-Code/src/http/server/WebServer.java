///A Simple Web Server (WebServer.java)

package http.server;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A webserver that store and manage some documents.
 * 
 * @author Rayane Belatche - Alexis Metwalli
 * @version 1.0
 */
public class WebServer {

  private static final String STATUS_500 = "500 Internal Server Error";

  private static final String RESOURCE_DIRECTORY = "files/public/";
  private static final String FILE_NOT_FOUND = "files/notfound.html";
  private static final String INDEX = "files/index.html";

  private void start(int port) {
    ServerSocket s;

    System.out.println("Webserver starting up on port " + port);
    System.out.println("(press CTRL+C to exit)");
    try {
      s = new ServerSocket(port);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    System.out.println("Waiting for connection");
    for (;;) {
      Socket remote = null;
      BufferedInputStream in = null;
      BufferedOutputStream out = null;
      try {
        remote = s.accept();
        System.out.println("Connection accepted, opening IO Streams");

        in = new BufferedInputStream(remote.getInputStream());
        out = new BufferedOutputStream(remote.getOutputStream());

        System.out.println("Waiting for data...");
        String header = new String();

        // header ends with \r\n\r\n (CR LF CR LF)
        int bcur = '\0', bprec = '\0';
        boolean newline = false;
        while((bcur = in.read()) != -1 && !(newline && bprec == '\r' && bcur == '\n')) {
          if(bprec == '\r' && bcur == '\n') {
            newline = true;
          } else if(!(bprec == '\n' && bcur == '\r')) {
            newline = false;
          }
          bprec = bcur;
          header += (char) bcur;
        }

        System.out.println("------REQUEST------");
        System.out.println("Header :");
        System.out.println(header);

        if(bcur != -1 && !header.isEmpty()) {
          String[] words = header.split(" ");
          String requestType = words[0];
          String resourceName = words[1].substring(1);

          if(requestType.equals("GET")) {
              if(resourceName.isEmpty()) {
                //If there is no resource in the URL we send the index file
                handleGET(out, INDEX);
              } else {
                handleGET(out, RESOURCE_DIRECTORY + resourceName);
              }

            } else if(requestType.equals("PUT")) {
              if(!resourceName.isEmpty()) {
                handlePUT(in, out, RESOURCE_DIRECTORY + resourceName);
              } else {
                out.write(makeHeader("400 Resource missing").getBytes());
                out.flush();
              }

            } else if(requestType.equals("POST")) {
            if(!resourceName.isEmpty()) {
              handlePOST(in, out, RESOURCE_DIRECTORY + resourceName);
            } else {
              out.write(makeHeader("400 Resource missing").getBytes());
              out.flush();
            }
            } else if(requestType.equals("HEAD")) {
              //HEAD method is the same ad GET but with fewer information
              if(resourceName.isEmpty()) {
                handleHEAD(out, INDEX);
              } else {
                handleHEAD(out, RESOURCE_DIRECTORY + resourceName);
              }

            } else if(requestType.equals("DELETE")) {
              if(!resourceName.isEmpty()) {
                handleDELETE(out, RESOURCE_DIRECTORY  + resourceName);
              } else {
                out.write(makeHeader("400 Resource missing").getBytes());
                out.flush();
              }

            } else {
              out.write(makeHeader("404 Status not found").getBytes());
              out.flush();
            }
        } else {
          out.write(makeHeader("400 Bad Request").getBytes());
          out.flush();
        }
        System.out.println("------------------");
        remote.close();
      } catch (Exception e) {
        e.printStackTrace();
        try {
          out.write(makeHeader(STATUS_500).getBytes());
          out.flush();
        } catch (Exception e2) {};
        try {
          remote.close();
        } catch (Exception e2) {}
      }
    }
  }

  /**
   * This method respond to a get request by sending the chosen file
   * @param out output stream
   * @param filename resource filename
   */
  private void handleGET(BufferedOutputStream out, String filename) {
    System.out.println("GET " + filename);
    try {
      File resource = new File(filename);
      if(resource.exists() && resource.isFile()) {
        out.write(makeHeader("200 OK", filename, resource.length()).getBytes());
      } else {
        resource = new File(FILE_NOT_FOUND);
        out.write(makeHeader("404 File Not Found", FILE_NOT_FOUND, resource.length()).getBytes());
      }

      BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(resource));

      byte[] buffer = new byte[256];
      int nbRead;
      while((nbRead = fileIn.read(buffer)) != -1) {
        out.write(buffer, 0, nbRead);
      }

      fileIn.close();
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
      try {
        out.write(makeHeader(STATUS_500).getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   * Respond to a HEAD request like a GET request but without its body
   * @param out output stream
   * @param filename resource filename
   */
  private void handleHEAD(BufferedOutputStream out, String filename) {
    System.out.println("HEAD " + filename);
    try {
      File resource = new File(filename);
      if(resource.exists() && resource.isFile()) {
        out.write(makeHeader("200 OK", filename, resource.length()).getBytes());
      } else {
        out.write(makeHeader("404 Not Found").getBytes());
      }
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
      try {
        out.write(makeHeader(STATUS_500).getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   * Respond to the PUT request by  adding the document in the correct repository and replacing it if already exists
   * @param in input stream for reading the binary file
   * @param out output stream
   * @param filename resource filename
   */
  private void handlePUT(BufferedInputStream in, BufferedOutputStream out, String filename) {
    System.out.println("PUT " + filename);
    try {
      File resource = new File(filename);
      boolean existed = resource.exists();

      //For the PUT method we replace the existing document if it exists by the new one
      PrintWriter pw = new PrintWriter(resource);
      pw.close();

      BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource));
      byte[] buffer = new byte[256];
      while(in.available() > 0) {
        int nbRead = in.read(buffer);
        fileOut.write(buffer, 0, nbRead);
      }
      fileOut.flush();
      fileOut.close();

      if(existed) {
        out.write(makeHeader("204 No Content").getBytes());
      } else {
        out.write(makeHeader("201 Created").getBytes());
      }
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.write(makeHeader(STATUS_500).getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   *  Responds to the POST request by creating a file in correct repository if it already doesn't exist
   * @param in input stream for reading the binary file
   * @param out output stream
   */
  private void handlePOST(BufferedInputStream in, BufferedOutputStream out, String filename) {
    System.out.println("POST ");
    try {

      File resource = new File(filename);
      boolean existed = resource.exists();

      BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource, existed));

      byte[] buffer = new byte[256];
      while(in.available() > 0) {
        int nbRead = in.read(buffer);
        fileOut.write(buffer, 0, nbRead);
      }
      fileOut.flush();
      fileOut.close();

      if(existed) {
        out.write(makeHeader("200 OK").getBytes());
      } else {
        out.write(makeHeader("201 Created").getBytes());
      }
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.write(makeHeader(STATUS_500).getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   * Responds to the DELETE request by deleting the chosen file
   * @param out output stream
   * @param filename name of the resource that needs to be deleted
   */
  private void handleDELETE(BufferedOutputStream out, String filename) {
    System.out.println("DELETE " + filename);
    try {
      File resource = new File(filename);
      boolean deleted = false;
      boolean existed = false;
      if((existed = resource.exists()) && resource.isFile()) {
        deleted = resource.delete();
      }

      if(deleted) {
        out.write(makeHeader("204 Content deleted").getBytes());
        out.write((filename + " has been deleted correctly.").getBytes());
      } else if (!existed) {
        out.write(makeHeader("404 Not Found").getBytes());
        out.write((filename + " doesn't exists").getBytes());
      } else {
        out.write(makeHeader("403 Forbidden").getBytes());
      }
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.write(makeHeader(STATUS_500).getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   * Write the header following the http standard
   * @param status response status
   * @return the header written
   */
  private String makeHeader(String status) {
    String header = "HTTP/1.0 " + status + "\r\n";
    header += "Server: Bot\r\n";
    header += "\r\n";
    System.out.println("Response Header :");
    System.out.println(header);
    return header;
  }

  /**
   * Write the header following the http standard
   * @param status response status
   * @param filename resource filename
   * @param length resource length
   * @return the header written
   */
  private String makeHeader(String status, String filename, long length) {
    String header = "HTTP/1.0 " + status + "\r\n";
    if(filename.endsWith(".html") || filename.endsWith(".htm"))
      header += "Content-Type: text/html\r\n";
    else if(filename.endsWith(".mp4"))
      header += "Content-Type: video/mp4\r\n";
    else if(filename.endsWith(".png"))
      header += "Content-Type: image/png\r\n";
    else if(filename.endsWith(".jpeg") || filename.endsWith(".jpg"))
      header += "Content-Type: image/jpg\r\n";
    else if(filename.endsWith(".mp3"))
      header += "Content-Type: audio/mp3\r\n";
    else if(filename.endsWith(".avi"))
      header += "Content-Type: video/x-msvideo\r\n";
    else if(filename.endsWith(".css"))
      header += "Content-Type: text/css\r\n";
    else if(filename.endsWith(".pdf"))
      header += "Content-Type: application/pdf\r\n";
    else if(filename.endsWith(".odt"))
      header += "Content-Type: application/vnd.oasis.opendocument.text\r\n";
    header += "Content-Length: " + length + "\r\n";
    header += "Server: Bot\r\n";
    header += "\r\n";
    System.out.println("Response Header :");
    System.out.println(header);
    return header;
  }


  /**
   * Starts the application.
   * 
   * @param args potential port number
   */
  public static void main(String args[]) {
    System.out.println("WebServer started, you can choose a specific port number by adding a parameter when running the program.");
    WebServer ws = new WebServer();
    int portNumber = 3000;
    if (args.length > 0) {
      portNumber = Integer.parseInt(args[0]);
    }
    ws.start(portNumber);
  }
}
