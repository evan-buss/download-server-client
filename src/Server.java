/*
 * Author: Evan Buss
 * Major: Computer Science
 * Creation Date: November 06, 2018
 * Due Date: November 19, 2018
 * Course: CSC328 - 020 Network Programming
 * Professor: Dr. Frye
 * Assignment: Download Client / Server
 * Filename: Server.java
 * Purpose:  Server daemon that waits for client connections.
 *           When a connection is received, the server creates
 *           a new thread to handle it. The client sends
 *           requests to navigate the server's directories and
 *           download files. The server sends these files over
 *           a TCP connection.
 * Language: Java 8 (1.8.0_101)
 * Compilation Command: javac Server.java
 * Execution Command: java Server [port]
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class Server {
  public static void main(String args[]) {

    ServerSocket server = null;

    try {
      if (args.length == 0) {
        // No port specified, create ServerSocket and bind to default port 50001
        server = new ServerSocket(50001);
      } else if (args.length == 1) {
        // Create ServerSocket on specified port
        server = new ServerSocket(Integer.parseInt(args[0]));
      } else {
        System.err.println("Usage: java Server [port]");
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

    System.out.println("Exiting Server...");
    try {
      server.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.exit(0);
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

    // infinite loop waiting for new connections in main thread'
    // Close server daemon with Ctrl-C
    //noinspection InfiniteLoopStatement
    while (true) {
      try {
        // Accept any incoming connections
        client = server.accept();
      } catch (IOException ex) {
        System.err.println("ServerSocket: Error Connecting with Client");
        ex.printStackTrace();
      }

      //    Only execute if client has actually connected (!= null)
      if (client != null) {
        // Create a new thread and pass it the client connection socket
        // connection
        Thread t = new Thread(new ClientConnection(client));
        t.start();
      }
    }

  }
}

/**
 * ClientConnection class is responsible for handling individual connections and
 * sending and receiving data from client to server and vice versa. Implements
 * the runnable interface so that each instance can be executed as a separate
 * thread.
 */
class ClientConnection implements Runnable {

  private final Socket client;
  private PrintWriter outStream;
  private BufferedReader inStream;

  /**
   * Constructor. Takes the connected client socket as a parameter.
   *
   * @param client socket that is connected with the client
   */
  ClientConnection(Socket client) {
    this.client = client;
  }

  /**
   * Default method implemented by the Runnable interface. It gets called when a
   * new instance of ClientConnection is created. Starts clientHandler() function
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

    boolean run = true;
    // Initialize the current directory file to the program's directory
    File currentDirectory = new File(System.getProperty("user.dir"));
    String rawInput = ""; // Full client request
    String parsedCommand = ""; // Command portion of client request

    // Create server input and output streams
    try {
      outStream = new PrintWriter(client.getOutputStream(), true);

      inStream = new BufferedReader(new InputStreamReader(client.getInputStream()));

    } catch (IOException ex) {
      System.err.println("Error Creating Input and Output Streams");
      ex.printStackTrace();
    }

    System.out.println("Server connected to a client at "
            + client.getInetAddress() + " on "
            + Thread.currentThread().getName());

    // Send client a message that they have successfully connected
    outStream.println("HELLO");

    // Loop until client chooses to exit
    while (run) {
      try {
        rawInput = inStream.readLine();

        // Get the command token (first word) from user input
        if (rawInput.split("\\s+").length >= 2) {
          parsedCommand = rawInput.split("\\s+")[0];
        } else {
          parsedCommand = rawInput;
        }

      } catch (IOException ex) {
        System.err.println("Error Reading From Input Stream");
        ex.printStackTrace();
      }

      switch (parsedCommand.toUpperCase()) {
        case "BYE":
          System.out.println("Closing client's connection from "
                  + client.getInetAddress() + " on "
                  + Thread.currentThread().getName());
          run = false; // break out of loop
          break;
        case "PWD":
          System.out.println(Thread.currentThread().getName() +
                  ": PWD Received");
          outStream.println(currentDirectory.getPath()); // send file's current path
          break;
        case "DIR":
          System.out.println("DIR Received");
          System.out.println(Thread.currentThread().getName() +
                  ": DIR Received");
          outStream.println(getDirectory(currentDirectory.getPath()));
          break;
        case "CD":
          System.out.println(Thread.currentThread().getName() +
                  ": CD Received");

          // Attempt to change directory and store output in String
          String output = changeDirectory(rawInput, currentDirectory);

          // There was an error navigating to new directory
          if (!output.equals("DDNE") && !output.equals("PD")) {
            currentDirectory = new File(output);
          }

          // Send the function response string regardless, the client will handle
          // displaying the error codes to the user
          outStream.println(output);

          break;
        case "DOWNLOAD":
          System.out.println(Thread.currentThread().getName() +
                  ": Download Received");
          sendFile(rawInput, currentDirectory, outStream, inStream);
          break;
        default:
          outStream.println("Client Request Error.");
          System.out.println(Thread.currentThread().getName() +
                  ": Client sent invalid command");
      }
    }

    // Close the client connection when finished.
    try {
      client.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Display the contents of the clients current working directory.
   * Separates the results by file and folder. Users can download files and enter
   * folders.
   *
   * @param directory absolute path String to a file directory
   * @return String delimited by "#" character between tokens. Each
   * file/folder has 3 different attributes.
   * Type: File or Folder
   * Size: File or Folder Size in Bytes
   * Name: File or Folder Name
   * The client will be responsible for parsing the tokens and
   * formatting the output.
   */
  private String getDirectory(String directory) {
    File folder = new File(directory);
    File[] directoryListing = folder.listFiles();
    StringBuilder output = new StringBuilder();

    /* Build a new string with files at the top and directories at the
       bottom. Delimit each entry with a "#" character. The client will
       parse the tokens from the long string that the server sends.
    */
    if (directoryListing != null) {
      for (File file : directoryListing) {
        if (file.isFile()) {
          output.insert(0, "File#" +
                  +file.length() +
                  "#" + file.getName() + "#");
        } else if (file.isDirectory()) {
          output.append("Folder");
          output.append("#");
          output.append(file.length());
          output.append("#");
          output.append(file.getName());
          output.append("#");
        }
      }

      // If length is 0, the folder was empty
      if (output.length() == 0) {
        output.append("EMPTY");
      }
    }

    return output.toString();
  }

  /**
   * Reads the client request and parses the new directory path given. Makes sure
   * that the given path exists, is a directory, and the server has read permissions
   * Return the path if success, otherwise return an error code.
   *
   * @param rawInput         the entire request string from the client
   * @param currentDirectory the server's current working directory for the client
   * @return Returns the new file's absolute path on success.
   * Returns "PD" if user does not have read permissions
   * Returns "DDNE" if the directory doesn't exist
   */
  private String changeDirectory(String rawInput,
                                 File currentDirectory) {
    File newFilePath;

    //Create a new string excluding the "CD" part of the array
    String targetDir = rawInput.substring(rawInput.indexOf(" ")).trim();


    //FIXME: Make sure this logic is correct for the two different path types
    //Go up a level to parent directory
    if (targetDir.equals("..")) {
      newFilePath = new File(currentDirectory.getParent());
      // If input contains a "/", user entered a full file path
    } else if (targetDir.contains(File.separator)) {
      newFilePath = new File(targetDir);
    } else { // Input is a relative path
      newFilePath = new File(currentDirectory, targetDir);
    }

    // Check if the input file/directory exists, is a directory, and has read permissions
    if (newFilePath.exists() && newFilePath.isDirectory() && newFilePath.canRead()) {
      return newFilePath.getAbsolutePath(); // Success, return new
    } else if (!newFilePath.canRead()) {
      return "PD";
    } else {
      return "DDNE";
    }
  }

  private void sendFile(String rawInput, File directory,
                        PrintWriter outStream, BufferedReader inStream) {

    // Create a new string excluding the command
    String fileName = rawInput.substring(rawInput.indexOf(" ")).trim();

    BufferedInputStream fileReader = null;
    OutputStream bytesOut;
    int bytesSent;
    byte[] buffer = new byte[1000000]; // Can transfer 1mb at a time

    // Set file to the filename the user gives in their request
    File file = new File(directory, fileName);

    /* Make sure that given file exists, is a file not a directory, and the
       server has read permissions for the file */
    if (file.isFile() && file.exists() && file.canRead()) {
      try {
        // Create a new stream to read from source file
        fileReader = new BufferedInputStream(new FileInputStream(file));

      } catch (FileNotFoundException e) {
        System.err.println("Could not create file reader stream");
        e.printStackTrace();
      }

      // Tell the client that the server is ready to send the file
      outStream.println("READY");

      try {
        // Get client response after sending "READY"
        if (inStream.readLine().equals("READY")) {
          bytesOut = client.getOutputStream();

          // Send the file length before sending the file
          outStream.println(file.length());

          // Make sure fileReader stream has been initialized before reading
          if (fileReader != null) {
            // Keep reading from file and sending to client until all data is sent or error
            while ((bytesSent = fileReader.read(buffer, 0, buffer.length)) != -1) {
              bytesOut.write(buffer, 0, bytesSent);
            }

            System.out.println(file.getName() + " sent to client successfully!");
            fileReader.close(); // Close the file input stream
            bytesOut.flush();   // Flush the data output stream
          }

        } else {
          System.out.println("Client has aborted the download");
        }
      } catch (IOException e) {
        System.err.println("Download could get client response.");
        e.printStackTrace();
      }
    } else {
      outStream.println("FNF");
    }
  }
}

