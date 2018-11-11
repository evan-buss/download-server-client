/*
 * Author: Evan Buss
 * Major: Computer Science
 * Creation Date: November 06, 2018
 * Due Date: November 19, 2018
 * Course: CSC328 - 020 Network Programming
 * Professor: Dr. Frye
 * Assignment: Download Client / Server
 * Filename: Client.java
 * Purpose:  Client that sends requests to the download server.
 * Language: Java 8 (1.8.0_101)
 * Compilation Command: javac Client.java
 * Execution Command: java Client <host> [port]
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    public static void main(String args[]) {

        if (args.length == 1) {
            // User only entered a hostname
            connect(args[0], 50001);
        } else if (args.length == 2) {
            // User entered hostname and port number
            connect(args[0], Integer.parseInt(args[1]));
        } else {
            System.err.println("Usage: java Client <host> [port]");
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

        // Create a new TCP socket and try to connect to server
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

        String rawInput; // The unmodified string from the user's input
        String parsedCommand; // The parsed command from rawInput
        String currentPath = "~";
        // String directory = "";
        boolean run = true;

        // Connection is complete, read message from server (should be "HELLO")
        try {
            System.out.println("Message from Server: " + inStream.readLine());
        } catch (IOException ex) {
            System.out.println("Error Receiving Welcome Message from Server");
            ex.printStackTrace();
            System.exit(-1);
        }

        // Loop forever, read user inputs from command line
        while (run) {
            shellMenu(currentPath);
            rawInput = keyboard.nextLine();

            // If the input has multiple parameters, split it
            if (rawInput.split("\\s+").length > 1) {
                parsedCommand = rawInput.split("\\s+")[0];
            } else {
                parsedCommand = rawInput;
            }


            switch (parsedCommand.toUpperCase()) {
                case "BYE":
                    outStream.println(parsedCommand);
                    run = false;
                    System.out.println("Exiting program.");
                    break;
                case "PWD":
                    outStream.println(parsedCommand);

                    // Set the bash prompt to the current server directory
                    try {
                        currentPath = inStream.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "DIR":
                    outStream.println(parsedCommand);
                    try {
                        /* PROTOCOL: The response string will be broken into
                        multiple lines and use the '/' char to indicate a
                        newline. The client will translate these characters
                        into new lines. */

                        String response = inStream.readLine();
                        response = response.replace('/', '\n');
                        System.out.println("Type         Name");
                        System.out.println("----         ----");
                        System.out.println(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "CD":
                    String output = changeDirectory(rawInput, outStream,
                            inStream);
                    if (!output.equals("ERROR")) {
                        currentPath = output;
                    }
                    break;
                case "DOWNLOAD":
                    System.out.println("Download shit here");
                    break;
                case "HELP":
                    displayHelpMenu();
                    break;
                default:
                    System.out.println("Invalid command. Type HELP to learn more.");
            }
        }
    }

    private static String changeDirectory(String rawInput,
                                          PrintWriter outStream,
                                          BufferedReader inStream) {

        String response;
        // Make sure that the client input has at least a single argument
        if (rawInput.split("\\s+").length >= 2) {
            outStream.println(rawInput); // Send server command + argument
            try {
                // response should be the new directory string
                response = inStream.readLine();

                // Get response. Translate error codes if present.
                switch (response) {
                    case "DDNE":
                        System.out.println("CD Error: Directory does not exist.");
                        return "ERROR";
                    case "PD":
                        System.out.println("CD Error: Directory Access " +
                                "Permission Denied");
                        return "ERROR";
                    default:
                        return response;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Otherwise the input is invalid
        } else {
            System.out.println("Invalid usage of CD command. Type HELP to learn more.");
        }
        return "ERROR";
    }

    /**
     * Displays list of all client commands and their usage.
     */
    private static void displayHelpMenu() {

        StringBuilder string = new StringBuilder();

        string.append("Valid Server Commands:\n\n");
        string.append("HELP\n");
        string.append("\tDisplays list of all available commands\n\n");
        string.append("BYE\n");
        string.append("\tDisconnects from the server and closes client\n\n");
        string.append("PWD\n");
        string.append("\tDisplays your current directory on the server\n\n");
        string.append("DIR\n");
        string.append("\tDisplays file and folder listings of the current " +
                "directory\n\n");
        string.append("CD <absolute/relative directory>\n");
        string.append("\tNavigate to the specified directory.\n" +
                "\tType .. to move up a directory\n\n");
        string.append("DOWNLOAD <filename>\n");
        string.append("\tNavigate to the specified directory.\n" +
                "\tType .. to move up a directory\n\n");

        System.out.println(string);
    }

    /**
     * Displays an interactive shell menu where the user can type commands.
     *
     * @param str a String that shows the user what server directory they are in
     */
    private static void shellMenu(String str) {
        System.out.println(str);
        System.out.print("> ");
    }
}
