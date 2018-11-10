/*
 * Author: Evan Buss
 * Major: Computer Science
 * Creation Date: November 06, 2018
 * Due Date: November 19, 2018
 * Course: CSC328 - 020 Network Programming
 * Professor: Dr. Frye
 * Assignment: Download Client / Server
 * Filename: Server.java
 * Purpose:  Multi-threaded server that allows users to
 *           traverse its directories and download files
 * Language: Java 8 (1.8.0_101)
 * Compilation Command: javac Server.java
 * Execution Command: java Server [port]
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
        // Create ServerSocket on default port 50001
        server = new ServerSocket(50001);
      } else if (args.length == 1) {
        // Create ServerSocket on specified port
        server = new ServerSocket(Integer.parseInt(args[0]));
      } else {
        System.err.println("Usage: java Server [port]\n\t" +
            "or java -jar Server.jar [port]");
        System.exit(-1);
      }
    } catch (IOException ex) {
      System.err.println("Could not create a ServerSocket");
      ex.printStackTrace();
      System.exit(-1);
    }

    System.out.println("Server Listening on: " + server.getLocalPort());

    // Server is bound to a port, wait for connections in a loop
    waitForConnection(server);
  }


  /**
   * Waits for incoming connections with the server. Once a client connection
   * is received, it accepts it and then creates a new thread to handle the
   * connection. This allows multiple clients to connect to the server
   * simultaneously
   *
   * @param server server socket that is bound to a port number
   */
  private static void waitForConnection(ServerSocket server) {

    Socket client = null;

    // infinite loop waiting for new connections
    while (true) {

      try {
        // Accept any incoming connections
        client = server.accept();
      } catch (IOException ex) {
        System.err.println("Error Connecting with Client");
        ex.printStackTrace();
      }

      //    Only execute if client has actually connected (!= null)
      if (client != null) {
        // Create a new thread and start it passing in the client socket
        // connection
        Thread t = new Thread(new ClientConnection(client));
        t.start();
      }
    }
  }
}

/**
 * ClientConnection class is responsible for handling all client connections
 * and sending and receiving data from client to server and vice versa.
 * Implements the runnable interface so that each instance can be executed as a
 * separate thread.
 */
class ClientConnection implements Runnable {

  private Socket client;
  private PrintWriter outStream;
  private BufferedReader inStream;
  private String userInput;

  /**
   * Constructor. Takes the connected client socket as a parameter.
   *
   * @param client socket that is connected with the client
   */
  ClientConnection(Socket client) {
    this.client = client;
  }

  /**
   * Default method implemented by the Runnable interface. It gets called
   * when a new instance of ClientConnection is created.
   */
  public void run() {
    clientHandler();
  }

  /**
   * Responsible for sending and receiving data from a single client. Loops
   * indefinitely until client sends a disconnect request. Calls appropriate
   * functions for other client commands.
   */
  private void clientHandler() {

    // Create server input and output streams
    try {
      outStream = new PrintWriter(client.getOutputStream(), true);

      inStream =
          new BufferedReader(new InputStreamReader(client.getInputStream()));

    } catch (IOException ex) {
      System.err.println("Error Creating Input and Output Streams");
      ex.printStackTrace();
    }

    System.out.println("Server connected to a client at "
        + client.getInetAddress() + " on "
        + Thread.currentThread().getName());

    // Send client a message that they have successfully connected
    outStream.println("HELLO");

    while (true) {
      try {
        userInput = inStream.readLine();
      } catch (IOException ex) {
        System.err.println("Error Reading From Input Stream");
        ex.printStackTrace();
      }

      if (userInput.equals("BYE")) {
        System.out.println("Closing client's connection from "
            + client.getInetAddress() + " on "
            + Thread.currentThread().getName());
        break;
      }
    }

    // Close the client connection when finished.
    try {
      client.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
