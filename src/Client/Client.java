/*
 * Author: Evan Buss
 * Major: Computer Science
 * Creation Date: November 06, 2018
 * Due Date: November 19, 2018
 * Course: CSC328 - 020 Network Programming
 * Professor: Dr. Frye
 * Assignment: Download Client / Server
 * Filename: Client.java
 * Purpose:  Client that sends requests to the download
 * server
 * Language: Java 8 (1.8.0_101)
 * Compilation Command: javac Client.java
 * Execution Command: java Client hostname <port>
 */

package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class Client {
  public static void main(String args[]) {

    try {
      sleep(2000);
    } catch (InterruptedException ex) {
      System.err.println("Sleep Error");
      ex.printStackTrace();
    }

    if (args.length == 1) {
//      User entered just hostname
      connect(args[0], 0);
    } else if (args.length == 2) {
//      user Entered hostname  and port number
      connect(args[0], Integer.parseInt(args[1]));
    } else {
//      FIXME: Confirm proper way to show optional arguments
      System.err.println("Usage: java Client hostname <port>\n\t" +
          "or java -jar Client.jar hostname <port>");
    }
  }

  /**
   * Creates a TCP connection using the given hostname and port number
   *
   * @param hostname the server name or IP address to connect to
   * @param port     the port number to connect to; 0 means choose any open
   *                 port
   */
  private static void connect(String hostname, int port) {

    Socket sock = null;

//    Create a new TCP socket and try to connect to server
    try {
      if (port == 0) { // Connect to default port
        sock = new Socket(hostname, 50001);
      } else { // Connect to specified port
        sock = new Socket(hostname, port);
      }
    } catch (UnknownHostException ex) {
      System.err.println("Unknown Host. Connection failed.");
      ex.printStackTrace();
      System.exit(-1);
    } catch (IOException ex) {
      System.err.println("IOException");
      ex.printStackTrace();
      System.exit(-1);
    }

    clientLoop(sock);

  }

  private static void clientLoop(Socket sock) {

    PrintWriter outStream = null;
    BufferedReader inStream = null;
    Scanner userStream = null;
    String userInput;

//    First, setup streams for TCP in/out and user input
    try {
      outStream = new PrintWriter(sock.getOutputStream(), true);

      inStream =
          new BufferedReader(new InputStreamReader(sock.getInputStream()));

      userStream = new Scanner(System.in);
//      BufferedReader userInput =
//          new BufferedReader(new InputStreamReader(System.in));
    } catch (IOException ex) {
      System.err.println("Error Creating Input and Output Streams");
      ex.printStackTrace();
      System.exit(-1);
    }

    try {
      System.out.println("Message from Server: " + inStream.readLine());
    } catch (IOException ex) {
      System.out.println("Error Receiving Welcome Message from Server");
      ex.printStackTrace();
    }

//    TODO: Think about ways to handle different user inputs
    while (true) {
      displayMenu("");
      userInput = userStream.nextLine();

      if (userInput.equals("BYE")) {
        outStream.println(userInput);
        System.out.println("Exiting program.");
        break;
      } else {
        outStream.println(userInput);
      }
    }


  }

  private static void displayMenu(String str) {
    System.out.println("$");
    System.out.print("> ");
  }
}
