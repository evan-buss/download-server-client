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


//    FIXME: This is for testing purposes only
    try {
      sleep(2000);
    } catch (InterruptedException ex) {
      System.err.println("Sleep Error");
      ex.printStackTrace();
    }

    if (args.length == 1) {
//      User entered just hostname
      connect(args[0], 50001);
    } else if (args.length == 2) {
//      user Entered hostname  and port number
      connect(args[0], Integer.parseInt(args[1]));
    } else {
      System.err.println("Usage: java Client <host> [port]\n\t" +
          "or java -jar Client.jar <host> [port]");
    }
  }

  /**
   * Creates a TCP connection using the given hostname and port number
   *
   * @param hostname the server name or IP address to connect to
   * @param port     the port number to connect to
   */
  private static void connect(String hostname, int port) {

    Socket sock = null;

//    Create a new TCP socket and try to connect to server
    try {
      sock = new Socket(hostname, port);
    } catch (UnknownHostException ex) {
      System.err.println("Unknown Host. Connection Failed.");
      ex.printStackTrace();
      System.exit(-1);
    } catch (IOException ex) {
      System.err.println("Socket Creation IOException");
      ex.printStackTrace();
      System.exit(-1);
    }


    // Setup streams for server input and output as well as user input
    try {
      PrintWriter outStream = new PrintWriter(sock.getOutputStream(),
          true);

      BufferedReader inStream =
          new BufferedReader(new InputStreamReader(sock.getInputStream()));

      Scanner keyboard = new Scanner(System.in);
      //  BufferedReader userInput =
      //  new BufferedReader(new InputStreamReader(System.in));

      // Connection succeeded, loop indefinitely and accept user input
      clientLoop(outStream, inStream, keyboard);

      sock.close();

    } catch (IOException ex) {
      System.err.println("Error Creating IO Streams");
      ex.printStackTrace();
      System.exit(-1);
    }


  }

  /**
   * Loops indefinitely until user enters BYE command. Parses user inputs and
   * sends server only valid protocol commands.
   *
   * @param outStream PrintWriter output stream connected to the server
   * @param inStream  BufferedReader input stream connected to the server
   * @param keyboard  Scanner connected to System.in in order to get user
   *                  input
   */
  private static void clientLoop(PrintWriter outStream,
                                 BufferedReader inStream, Scanner keyboard) {

    String userInput;
    String currentPath = "~";


    // Connection is complete, read message from server (should be "HELLO")
    try {
      System.out.println("Message from Server: " + inStream.readLine());
    } catch (IOException ex) {
      System.out.println("Error Receiving Welcome Message from Server");
      ex.printStackTrace();
      System.exit(-1);
    }

    // Loop forever, read user inputs from command line
    while (true) {
      displayMenu(currentPath);
      userInput = keyboard.nextLine().toUpperCase();

      // User wishes to leave,
      if (userInput.equals("BYE")) {
        outStream.println(userInput);
        System.out.println("Exiting program.");
        break;
      } else if (userInput.equals("HELP")) {
        displayHelpMenu();
      } else {
        System.out.println("Invalid command. Type HELP to learn more.");
      }
    }




  }

  /**
   * Displays list of all client commands and their usage.
   */
  private static void displayHelpMenu() {

    StringBuilder string = new StringBuilder();

    string.append("Valid Server Commands:\n\n");
    string.append("HELP\n");
    string.append("\t\tDisplays list of all available commands\n\n");
    string.append("BYE\n");
    string.append("\t\tDisconnects from the server and closes client\n\n");

    System.out.println(string);
  }


  /**
   * Displays an interactive shell menu where the user can type commands.
   *
   * @param str a String that shows the user what server directory they are in
   */
  private static void displayMenu(String str) {
    System.out.println(str + " $ ");
    System.out.print("> ");
  }
}
