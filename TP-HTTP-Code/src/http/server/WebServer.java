///A Simple Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 * 
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 * 
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {
  /**Chemin relatif du repertoire de ressources du serveur*/
  protected static final String RESOURCE_DIRECTORY = "files";
  /**Chemin relatif de la page web à envoyer en cas d'erreur 404*/
  protected static final String FILE_NOT_FOUND = "files/notfound.html";
  /**Chemin relatif de la page d'acceuil du serveur*/
  protected static final String INDEX = "files/win.html";
  /**
   * WebServer constructor.
   */
  protected void start(int port) {
    ServerSocket s;

    System.out.println("Webserver starting up on port " + port);
    System.out.println("(press CTRL+C to exit)");
    try {
      // Cr�ation du ServerSocket pour �couter sur le port port
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
        // Attente d'une connexion
        remote = s.accept();
        // Connexion accept�e !
        System.out.println("Connection accepted, opening IO Streams");
        // Ouverture de flux de lecture / �criture binaires pour le socket client
        in = new BufferedInputStream(remote.getInputStream());
        out = new BufferedOutputStream(remote.getOutputStream());

        // Lecture du Header (il se termine par une ligne vide)
        System.out.println("Waiting for data...");
        String header = new String();

        // Le header se termine par la s�quence \r\n\r\n (CR LF CR LF)
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

        System.out.println("REQUEST :");
        System.out.println(header);
        // Ici si bcur == -1 il y a une erreur, le protocole n'a pas �t� respect� (le Header ne se termine pas par une ligne vide)

        if(bcur != -1 && !header.isEmpty()) {
          String[] words = header.split(" ");
          String requestType = words[0];
          String resourceName = words[1].substring(1, words[1].length());
          // Par d�faut, envoyer la page d'acceuil
          if(resourceName.isEmpty()) {
            httpGET(out, INDEX);
            // On v�rifie que les requ�tes portent sur des ressources situ�es dans le r�pertoire de ressources !! Question de s�curit�.
          } else if(resourceName.startsWith(RESOURCE_DIRECTORY)) {
            // On redirige la requ�te vers le service appropri�
            if(requestType.equals("GET")) {
              httpGET(out, resourceName);
            } else if(requestType.equals("PUT")) {
              httpPUT(in, out, resourceName);
            } else if(requestType.equals("POST")) {
              httpPOST(in, out, resourceName);
            } else if(requestType.equals("HEAD")) {
              httpHEAD(in, out, resourceName);
            } else if(requestType.equals("DELETE")) {
              httpDELETE(out, resourceName);
            } else {
              // Si la requ�te ne correspond � rien de connu
              out.write(makeHeader("501 Not Implemented").getBytes());
              out.flush();
            }
          } else {
            // Interdiction d'acc�der � des ressources en dehors du dossier RESOURCE_DIRECTORY
            out.write(makeHeader("403 Forbidden").getBytes());
            out.flush();
          }
        } else {
          out.write(makeHeader("400 Bad Request").getBytes());
          out.flush();
        }
        // Fermeture de la connexion et des flux de lecture / �criture
        remote.close();
      } catch (Exception e) {
        e.printStackTrace();
        // En cas d'erreur on essaie d'avertir le client
        try {
          out.write(makeHeader("500 Internal Server Error").getBytes());
          out.flush();
        } catch (Exception e2) {};
        // En cas d'erreur, il faut s'assurer qu'on ait tent� au moins une fois de fermer la connexion
        try {
          remote.close();
        } catch (Exception e2) {}
      }
    }
  }


  protected void httpGET(BufferedOutputStream out, String filename) {
    System.out.println("GET " + filename);
    try {
      // V�rification de l'existence de la ressource demand�e
      File resource = new File(filename);
      if(resource.exists() && resource.isFile()) {
        // Envoi du Header signalant un succ�s
        out.write(makeHeader("200 OK", filename, resource.length()).getBytes());
      } else {
        // Si la ressource n'existe pas, on va plut�t envoyer une page d'erreur
        resource = new File(FILE_NOT_FOUND);
        // Envoi du Header signalant une erreur
        out.write(makeHeader("404 Not Found", FILE_NOT_FOUND, resource.length()).getBytes());
      }

      // Ouverture d'un flux de lecture binaire sur le fichier demand�
      BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(resource));
      // Envoi du corps : le fichier (page HTML, image, vid�o...)
      byte[] buffer = new byte[256];
      int nbRead;
      while((nbRead = fileIn.read(buffer)) != -1) {
        out.write(buffer, 0, nbRead);
      }
      // Fermeture du flux de lecture
      fileIn.close();

      //Envoi des donn�es
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
      // En cas d'erreur on essaie d'avertir le client
      try {
        out.write(makeHeader("500 Internal Server Error").getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   * Impl�mentation de la m�thode HTTP HEAD.
   * Cette m�thode v�rifie l'existence de la ressource, et retourne un en-t�te qui contient les m�mes informations que la requ�te GET correspondante.
   * Le code de retour peut �tre 200 OK si le fichier a �t� trouv�, ou 404 Not Found si le fichier n'a pas �t� trouv�.
   * La m�thode HEAD ne renvoie pas de corps.
   * @param in Flux de lecture binaire sur le socket client.
   * @param out Flux d'�criture binaire vers le socket client auquel il faut envoyer une r�ponse.
   * @param filename Chemin du fichier sur lequel le client veut obtenir des informations.
   */
  protected void httpHEAD(BufferedInputStream in, BufferedOutputStream out, String filename) {
    System.out.println("HEAD " + filename);
    try {
      // V�rification de l'existence de la ressource demand�e
      File resource = new File(filename);
      if(resource.exists() && resource.isFile()) {
        // Envoi du Header signalant un succ�s (pas besoin de corps)
        out.write(makeHeader("200 OK", filename, resource.length()).getBytes());
      } else {
        // Envoi du Header signalant une erreur (pas besoin de corps)
        out.write(makeHeader("404 Not Found").getBytes());
      }
      // Envoi des donn�es
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
      // En cas d'erreur on essaie d'avertir le client
      try {
        out.write(makeHeader("500 Internal Server Error").getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   *
   * @param in Flux de lecture binaire sur le socket client.
   * @param out Flux d'�criture binaire vers le socket client auquel il faut envoyer une r�ponse.
   * @param filename Chemin du fichier � cr�er (ou �craser).
   */
  protected void httpPUT(BufferedInputStream in, BufferedOutputStream out, String filename) {
    System.out.println("PUT " + filename);
    try {
      File resource = new File(filename);
      boolean existed = resource.exists();

      // Efface le contenu fichier avant de le remplacer par les informations re�ues
      PrintWriter pw = new PrintWriter(resource);
      pw.close();

      // Ouverture d'un flux d'�criture binaire vers le fichier
      BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource));

      // Lecture du body du
      byte[] buffer = new byte[256];
      while(in.available() > 0) {
        int nbRead = in.read(buffer);
        fileOut.write(buffer, 0, nbRead);
      }
      fileOut.flush();

      //Fermeture du flux d'�criture vers le fichier
      fileOut.close();

      // Envoi du Header (pas besoin de corps)
      if(existed) {
        // Ressource modifi�e avec succ�s, aucune information suppl�mentaire � fournir
        out.write(makeHeader("204 No Content").getBytes());
      } else {
        // Ressource cr��e avec succ�s
        out.write(makeHeader("201 Created").getBytes());
      }
      // Envoi des donn�es
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
      // En cas d'erreur on essaie d'avertir le client
      try {
        out.write(makeHeader("500 Internal Server Error").getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   * Impl�mentation de la m�thode HTTP POST.
   * Cette m�thode tente d'ajouter des informations � la fin d'une ressource existante, les informations � ajouter �tant les donn�es du corps de la requ�te re�ue.
   * Si la ressource existait d�j� sur le serveur, les donn�es sont ajout�es � la fin et l'en-t�te de la r�ponse envoy�e a un code de 200 OK.
   * Si la ressource n'existait pas, elle est cr��e et l'en-t�te de la r�ponse envoy�e a un code de 201 Created.
   * La r�ponse ne contient pas de corps.
   * @param in Flux de lecture binaire sur le socket client.
   * @param out Flux d'�criture binaire vers le socket client auquel il faut envoyer une r�ponse.
   * @param filename Chemin du fichier auquel il faut rajouter des informations (ou qu'il faut cr�er).
   */
  protected void httpPOST(BufferedInputStream in, BufferedOutputStream out, String filename) {
    System.out.println("POST " + filename);
    try {
      File resource = new File(filename);
      boolean existed = resource.exists();

      // Ouverture d'un flux d'�criture binaire vers le fichier, en mode insertion � la fin
      BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource, existed));

      // Recopie des informations re�ues dans le fichier
      byte[] buffer = new byte[256];
      while(in.available() > 0) {
        int nbRead = in.read(buffer);
        fileOut.write(buffer, 0, nbRead);
      }
      fileOut.flush();

      //Fermeture du flux d'�criture vers le fichier
      fileOut.close();

      // Envoi du Header
      if(existed) {
        // Ressource modifi�e avec succ�s
        out.write(makeHeader("200 OK").getBytes());
      } else {
        // Ressource cr��e avec succ�s
        out.write(makeHeader("201 Created").getBytes());
      }
      // Envoi des donn�es
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
      // En cas d'erreur on essaie d'avertir le client
      try {
        out.write(makeHeader("500 Internal Server Error").getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   * Impl�mentation de la m�thode HTTP DELETE.
   * Cette m�thode tente de supprimer la ressource d�sign�e par le chemin filename.
   * Si la ressource a bien �t� supprim�e, l'en-t�te de la r�ponse envoy�e contient le code 204 No Content.
   * SI la ressource n'existait pas, l'en-t�te de la r�ponse envoy�e contient le code 404 Not Found.
   * Si la ressource existait mais n'a pas pu �tre supprim�e, l'en-t�te de la r�ponse contient le code 403 Forbidden.
   * @param out Flux d'�criture binaire vers le socket client auquel il faut envoyer une r�ponse.
   * @param filename Chemin du fichier � supprimer.
   */
  protected void httpDELETE(BufferedOutputStream out, String filename) {
    System.out.println("DELETE " + filename);
    try {
      File resource = new File(filename);
      // Suppression du fichier
      boolean deleted = false;
      boolean existed = false;
      if((existed = resource.exists()) && resource.isFile()) {
        deleted = resource.delete();
      }

      // Envoi du Header
      if(deleted) {
        // Le fichier a �t� supprim�, aucune information suppl�mentaire � fournir
        out.write(makeHeader("204 No Content").getBytes());
      } else if (!existed) {
        // Le fichier n'a pas �t� trouv�
        out.write(makeHeader("404 Not Found").getBytes());
      } else {
        // Le fichier a �t� trouv� mais n'a pas pu �tre supprim�
        out.write(makeHeader("403 Forbidden").getBytes());
      }
      // Envoi des donn�es
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
      // En cas d'erreur on essaie d'avertir le client
      try {
        out.write(makeHeader("500 Internal Server Error").getBytes());
        out.flush();
      } catch (Exception e2) {};
    }
  }

  /**
   * Cette m�thode permet de cr�er un en-t�te de r�ponse HTML simple, pour une r�ponse qui n'a pas de corps.
   * L'en-t�te cr�� contient un code de retour et pr�cise le type du serveur : Bot.
   * @param status Le code de r�ponse HTML � fournir dans l'en-t�te.
   * @return L'en-t�te de r�ponse HTML.
   */
  protected String makeHeader(String status) {
    String header = "HTTP/1.0 " + status + "\r\n";
    header += "Server: Bot\r\n";
    header += "\r\n";
    System.out.println("ANSWER HEADER :");
    System.out.println(header);
    return header;
  }

  /**
   * Cette m�thode permet de cr�er un en-t�te de r�ponse HTML, pour une r�ponse qui aura un corps.
   * L'en-t�te cr�� contient un code de retour et pr�cise le type du serveur : Bot, le type de contenu du corps et la taille du corps en bytes.
   * @param status Le code de retour.
   * @param filename Le chemin vers la ressource associ�e � la r�ponse, dont le contenu sera renvoy� dans le corps de la r�ponse.
   * @param length La taille de la ressource en bytes.
   * @return L'en-t�te de r�ponse HTML.
   */
  protected String makeHeader(String status, String filename, long length) {
    String header = "HTTP/1.0 " + status + "\r\n";
    if(filename.endsWith(".html") || filename.endsWith(".htm"))
      header += "Content-Type: text/html\r\n";
    else if(filename.endsWith(".mp4"))
      header += "Content-Type: video/mp4\r\n";
    else if(filename.endsWith(".png"))
      header += "Content-Type: image/png\r\n";
    else if(filename.endsWith(".jpeg") || filename.endsWith(".jpeg"))
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
    System.out.println("ANSWER HEADER :");
    System.out.println(header);
    return header;
  }

  /**
   * Start the application.
   * 
   * @param args
   *            Command line parameters are not used.
   */
  public static void main(String args[]) {
    WebServer ws = new WebServer();
    ws.start(3000);
  }
}
