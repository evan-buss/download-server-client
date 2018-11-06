/*
 * Author: Evan Buss
 * Major: Computer Science
 * Creation Date: November 06, 2018
 * Due Date: November 19, 2018
 * Course: CSC328 - 020 Network Programming
 * Professor: Dr. Frye
 * Assignment: Download Client / Server
 * Filename: Server.java
 * Purpose:  Server that sends files to clients
 * Language: Java 8 (1.8.0_101)
 * Compilation Command: javac Server.java
 * Execution Command: java Server <port>
 */

package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
  public static void main(String args[]) {

    ServerSocket server = null;

    try {
      if (args.length == 0) {
//        Create ServerSocket on any free port
        server = new ServerSocket(0);
      } else if (args.length == 1) {
//        Create ServerSocket on specified port
        server = new ServerSocket(Integer.parseInt(args[0]));
      } else {
        System.err.println("Usage: java Server <port>\n\t" +
            "or java -jar Server.jar <port>");
        System.exit(-1);
      }
    } catch (IOException ex) {
      System.err.println("ServerSocket Creation Error");
      ex.printStackTrace();
      System.exit(-1);
    }

    System.out.println("Server Listening on: " + server.getLocalPort());

//    Server is bound to a port, wait for connections
    waitForConnection(server);

  }

  private static void waitForConnection(ServerSocket server) {

    Socket client = null;

    try {
      //    Accept any incoming connections
      client = server.accept();
    } catch (IOException ex) {
      System.err.println("Error Connecting with Client");
      ex.printStackTrace();
    }

    //    Only execute if client has actually connected (!= null)
    if (client != null) {
      System.out.println("Client connected from " + client.getInetAddress());

      //    Create a new thread and start it
      new Thread(new ClientConnection(client)).start();
    }

  }

}

//Thread class
class ClientConnection implements Runnable {

  private Socket client;
  private PrintWriter outStream;
  private BufferedReader inStream;
  private String userInput, serverOutput;
  private boolean clientConnected = true;

  //  takes the client connection socket as a parameter
  ClientConnection(Socket client) {
    this.client = client;
  }

  public void run() {
    clientHandler();
  }

  private void clientHandler() {
    try {
      outStream = new PrintWriter(client.getOutputStream(), true);

      inStream =
          new BufferedReader(new InputStreamReader(client.getInputStream()));
    } catch (IOException ex) {
      System.err.println("Error Creating Input and Output Streams");
      ex.printStackTrace();
    }

//    Send client a message that they have successfully connected
    outStream.println("HELLO");

//    TODO: Think about ways to validate user inputs
    while (true) {
      System.out.println("Loop");

      try {
        userInput = inStream.readLine();
        System.out.println("User Input: " + userInput);
      } catch (IOException ex) {
        System.err.println("Error Reading From Input Stream");
        ex.printStackTrace();
      }

      if (userInput.equals("BYE")) {
        break;
      }

      System.out.println("staying alive");
    }

  }

}
