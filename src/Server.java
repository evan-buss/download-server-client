import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

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

            // TODO: may change this to have a user command to exit instead
            // of looping forever
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
 * ClientConnection class is responsible for handling all client connections and
 * sending and receiving data from client to server and vice versa. Implements
 * the runnable interface so that each instance can be executed as a separate
 * thread.
 */
class ClientConnection implements Runnable {

    private Socket client;
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
     * new instance of ClientConnection is created.
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
        String rawInput = "";
        String parsedCommand = "";

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

        while (run) {

            try {
                rawInput = inStream.readLine();

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
                    run = false;
                    break;
                case "PWD":
                    System.out.println(Thread.currentThread().getName() +
                            ": PWD Received");
                    outStream.println(currentDirectory.getPath());
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

                    //Create a new string excluding the "CD" part of the array
                    String targetDir = rawInput.substring(rawInput.indexOf(" ")).trim();
                    System.out.println("Attempting to navigate to : " + targetDir);

                    // Attempt to change directory and store output in String
                    String output = changeDirectory(targetDir,
                            currentDirectory, outStream);

                    // There was an error navigating to new directory
                    if (!output.equals("DDNE")) {
                        currentDirectory = new File(output);
                    }
                    outStream.println(output);
                    break;
                case "DOWNLOAD":
                    System.out.println(Thread.currentThread().getName() +
                            ": Download Received");
                    // Create a new string excluding the command
                    String targetFile = rawInput.substring(rawInput.indexOf(" ")).trim();

                    sendFile(targetFile, currentDirectory, outStream, inStream);
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

    private void sendFile(String fileName, File directory,
                              PrintWriter outStream, BufferedReader inStream) {

        InputStream fileIn = null;
        DataOutputStream dataOut = null;
        String clientResponse = "";
        /*
        Logic: You have to open a local filestream on the file
                read from the filestream to an outputstream connected to the socket.
         */

        // Create a new file with the given filename
        File file = new File(directory, fileName)   ;

        if (file.isFile() && file.exists() && file.canRead()) {
            try {
                fileIn = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                System.out.println("File Not Found.");
                e.printStackTrace();
            }

            try {
                dataOut = new DataOutputStream(client.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            outStream.println("READY");

            try {
                 clientResponse = inStream.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (clientResponse.equals("READY")) {
            //    start downloading
                System.out.println("Client is ready for the file.");
                // Send client the length of the file
                outStream.println(file.length());

                byte[] data = new byte[8096];
                try {
                    System.out.println("sending the file");
                    int bytesRemaining = (int) file.length();
                    int bytesRead;
                    while (bytesRemaining > 0) {
                        bytesRead = fileIn.read(data);
                        bytesRemaining -= bytesRead;
                        System.out.println("Bytes Remaining: " + bytesRemaining);
                        dataOut.write(data, 0, bytesRead);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else {
                System.out.println("Client has aborted the file download.");
            }


        } else {
            outStream.println("FNF");
        }
    }

    private String changeDirectory(String clientInput,
                                   File currentDirectory,
                                   PrintWriter outStream) {
        File newFilePath;

        //Go up a level
        if (clientInput.equals("..")) {
            newFilePath = new File(currentDirectory.getParent());
        } else if (clientInput.contains(File.separator)) {
            newFilePath = new File(clientInput);
        } else {
            newFilePath = new File(currentDirectory, clientInput);
        }

        if (newFilePath.exists() && newFilePath.isDirectory() && newFilePath.canRead()) {
            return newFilePath.getAbsolutePath();
        } else if (!newFilePath.canRead()) {
            return "PD";
        } else {
            return "DDNE";
        }
    }

    private String getDirectory(String directory) {
        File folder = new File(directory);
        File[] directoryListing = folder.listFiles();
        StringBuilder output = new StringBuilder();

        /* Build a new string with files at the top and directories at the
            bottom. Replace all newline characters with the '/' char because
            linux and windows file names cannot contain it. This allows us to use
            println(). Otherwise the multiple newlines would break the string
            apart at the newline char when reading it with readLine() on the
            client.
        */
        if (directoryListing != null) {
            for (File file : directoryListing) {
                if (file.isFile()) {
                    output.insert(0, "File" + "         " + file.getName() +
                            "/");
                    // output.append(file.getName());
                } else if (file.isDirectory()) {
                    output.append("Folder");
                    output.append("       ");
                    output.append(file.getName());
                    output.append("/");
                }
            }
        }

        return output.toString();
    }
}

