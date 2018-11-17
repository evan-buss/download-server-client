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
 *           The client can navigate the server's directories
 *           and download files from the server to client machine.
 * Language: Java 8 (1.8.0_101)
 * Compilation Command: javac Client.java
 * Execution Command: java Client <host> [port]
 */

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

class Client {

  private static Socket sock = null;

  public static void main(String args[]) {

    if (args.length == 1) {
      // User only entered a hostname, connect to default port
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

      System.out.println("Exiting program.");

      // Close socket when user chooses to exit
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
    String parsedCommand; // The parsed command from rawInput (first word)
    String currentPath = "~"; // Placeholder until user gets server path (pwd)

    boolean run = true; // Loop conditional

    // Connection is complete, read message from server (should be "HELLO")
    try {
      System.out.println("Message from Server: " + inStream.readLine());
    } catch (IOException ex) {
      System.err.println("Error Receiving Welcome Message from Server");
      ex.printStackTrace();
      System.exit(-1);
    }

    // Loop until user chooses to exit
    while (run) {
      shellMenu(currentPath); // Set directory in shell display
      rawInput = keyboard.nextLine(); // Read user input from System.in

      // Get first word from multi-word inputs
      if (rawInput.split("\\s+").length > 1) {
        parsedCommand = rawInput.split("\\s+")[0];
      } else {
        parsedCommand = rawInput;
      }

      // Convert user's command to uppercase and test it for a match
      switch (parsedCommand.toUpperCase()) {
        case "BYE":
          outStream.println(parsedCommand); // Send "BYE" to server
          run = false; // Stop looping
          break;
        case "PWD":
          outStream.println(parsedCommand); // Send "PWD" to server

          // Update prompt to the current server directory
          try {
            currentPath = inStream.readLine();
          } catch (IOException e) {
            System.err.println("Could not read PWD response...");
            e.printStackTrace();
          }
          break;
        case "DIR":
          outStream.println(parsedCommand); // Send "DIR" to server
          try {
          /* Server response protocol:
              The server sends back a long string of various file
              attributes. This includes three things:
                  - Type (File or Folder)
                  - Size (File or Folder Size in Bytes)
                  - Name (Name of the File or folder)
              These attributes use a "#" character as a delimiter.
              It is up to the client to parse the output and display
              it in whatever format it wants.
           */

            String response = inStream.readLine();
            displayDirectory(response);
          } catch (IOException e) {
            e.printStackTrace();
          }
          break;
        case "CD":
          // Validate input. Save response to output string
          String output = changeDirectory(rawInput, outStream, inStream);

          // Display new directory if "CD" request is successful
          if (!output.equals("ERROR")) {
            currentPath = output;
          }
          break;
        case "DOWNLOAD":
          receiveFile(rawInput, keyboard, outStream, inStream);
          break;
        case "HELP":
          displayHelpMenu();
          break;
        default:
          System.out.println("Invalid command. Type HELP to learn more.");
      }
    }
  }

  /**
   * Sends a request to the download server to download a specific file. The server
   * will transfer the file to the client over a TCP connection.
   *
   * @param rawInput  Unformatted user input String that will be sent to the server
   * @param keyboard  Scanner that parses user input from the command line
   * @param outStream TCP socket stream to the server
   * @param inStream  TCP socket stream from the server
   */
  private static void receiveFile(String rawInput,
                                  Scanner keyboard,
                                  PrintWriter outStream,
                                  BufferedReader inStream) {

    String fileName;


    // Send server request and get response
    if (rawInput.split("\\s+").length >= 2) {
      outStream.println(rawInput);
      // Cut out command to get name of file to download
      fileName = rawInput.substring(rawInput.indexOf(" ")).trim();

      try {
        String response = inStream.readLine();
        if (response.equals("FNF")) {
          System.out.println("File could not be found on the server.");
          return;
        }
        //  Otherwise response is "READY" and we just continue as normal
      } catch (IOException e) {
        System.err.println("DOWNLOAD error. Could not get response from server");
        e.printStackTrace();
        return;
      }
    } else {
      System.out.println("Invalid usage of DOWNLOAD command. Type HELP to learn more.");
      return;
    }

    // Create new local file object before attempting to download
    // Downloaded files will be saved to the program's execution directory
    File newFile = new File(System.getProperty("user.dir"), fileName);
    byte[] buffer = new byte[1000000]; // File transfer buffer 1mb
    boolean readyToReceive = false;    // Set true when client is ready for download

    InputStream bytesIn = null;  // Receives data from server
    BufferedOutputStream fileWriter = null; // Outputs data to a file

    // Logic to prevent local file of the same name being overwritten
    // Check if the file already exists before downloading
    if (newFile.exists()) {
      System.out.print("File of the same name already exists locally. " +
              "Would you like to overwrite it? [y/n] ");

      // User chooses to overwrite the file.
      if (keyboard.nextLine().toLowerCase().equals("y")) {
        readyToReceive = true;
      } else {

        System.out.print("Would you like to save the file under a different name? [y/n] ");
        if (keyboard.nextLine().toLowerCase().equals("y")) {
          System.out.print("Enter the new filename: ");
          newFile = new File(System.getProperty("user.dir"), keyboard.nextLine());
          if (!newFile.exists()) {
            readyToReceive = true;
          } else {
            System.out.println("You chose another name that exists... Pathetic. Download aborted.");
          }
        } else {
          System.out.println("Download Cancelled ");
        }
      }
      //  File doesn't exist locally, so download normally
    } else {
      readyToReceive = true;
    }

    // Client is ready to proceed with file download
    if (readyToReceive) {

      // Send ready response to server
      outStream.println("READY");

      // Create output stream to new file
      try {
        fileWriter = new BufferedOutputStream(new FileOutputStream(newFile));
      } catch (FileNotFoundException e) {
        System.err.println("Could not create file output stream.");
        e.printStackTrace();
      }

      //FIXME: Can I use the existing buffered reader?
      // Create direct input stream from server. The existing buffered reader
      // is not good for file transfers
      try {
        bytesIn = sock.getInputStream();
      } catch (IOException e) {
        e.printStackTrace();
      }


      try {

        // Server sends the size of the file it will be sending
        int bytesRemaining = Integer.parseInt(inStream.readLine());
        System.out.println(newFile.getName() + " Stats:");
        System.out.println("\tSize: " + bytesRemaining + " bytes");
        System.out.println("\tLocation: " + newFile.getPath());

        // TODO: Implement a timer or print the download time or progress bar
        // System.out.println(Instant.now());

        // Make sure that fileWriter and bytesIn streams have been initialized
        if (fileWriter != null && bytesIn != null) {
          // Loop until all bytes of the incoming file have been successfully read
          while (bytesRemaining > 0) {

            //FIXME: Not sure what this null pointer exception error means
            int bytesRead = bytesIn.read(buffer, 0, buffer.length);
            if (bytesRead != -1) {
              bytesRemaining -= bytesRead; // Decrement bytesRemaining counter
              fileWriter.write(buffer, 0, bytesRead); // Write new data to file
            }
          }

          //FIXME: Not sure if I need to flush if I am closing right after
          // Flush and close the file output stream when finished
          fileWriter.flush();
          fileWriter.close();

        } else {
          System.err.println("Could not download because streams are null...");
        }

        // Display success message.
        // System.out.println(Instant.now());
        System.out.println("Download Finished!");

      } catch (IOException e) {
        System.err.println("Error while receiving file.");
        e.printStackTrace();
      }

    } else {
      System.out.println("Sending STOP to server");
      outStream.println("STOP");
    }
  }


  /**
   * Sends a request to the server to change the client's current directory.
   *
   * @param rawInput  Complete user input string containing the "CD" request
   *                  as well as any arguments
   * @param outStream TCP socket stream to the server
   * @param inStream  TCP socket stream from the server
   * @return Returns new directory string if "CD" request successful.
   * Else prints error to the console and returns "ERROR"
   */
  private static String changeDirectory(String rawInput,
                                        PrintWriter outStream,
                                        BufferedReader inStream) {

    // Make sure that "CD" request has at least a single argument
    if (rawInput.split("\\s+").length >= 2) {
      outStream.println(rawInput); // Send server command + argument
      try {
        // response should be the new directory string
        String response = inStream.readLine();

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
        System.err.println("CD request did not return a response.");
        e.printStackTrace();
      }
      // CD request with 1 argument is invalid
    } else {
      System.out.println("Invalid usage of CD command. Type HELP to learn more.");
    }
    return "ERROR";
  }


  /**
   * Parses a string in server response format (Tokens separated by "#" chars)
   * And displays the attributes for the user to see.
   *
   * @param str String response from the server
   */
  private static void displayDirectory(String str) {

    // Create a new scanner to get tokens between "#" character.
    Scanner parser = new Scanner(str).useDelimiter("#");
    System.out.println("Type      Size(b)       Name\n");
    System.out.println("----      -------       ----\n");

    if (str.equals("EMPTY")) {
      System.out.println("Empty directory.\n");
    } else {
      // Traverse entire string, printing out the attributes in groups of three
      while (parser.hasNext()) {
        System.out.printf("%-6s    %-10s    %s%n",
                parser.next(), parser.next(), parser.next());
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
    string.append("\tDownloads the specified file to ");
    string.append(System.getProperty("user.dir"));
    string.append("\n\tIf the file exists you will be prompted to overwrite, " +
            "rename, or cancel\n\tthe download\n");

    System.out.println(string);
  }

  /**
   * Displays a shell prompt menu where the user can type commands.
   *
   * @param str a String that shows the user what server directory they are in
   */
  private static void shellMenu(String str) {
    System.out.println(str);
    System.out.print("> ");
  }
}
