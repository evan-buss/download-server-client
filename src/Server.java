import java.io.*;
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

                if (rawInput.split("\\s+").length == 2) {
                    parsedCommand = rawInput.split("\\s+")[0];
                } else {
                    parsedCommand = rawInput;
                }

            } catch (IOException ex) {
                System.err.println("Error Reading From Input Stream");
                ex.printStackTrace();
            }

            switch (parsedCommand) {
                case "BYE":
                    System.out.println("Closing client's connection from "
                            + client.getInetAddress() + " on "
                            + Thread.currentThread().getName());
                    run = false;
                    break;
                case "PWD":
                    System.out.println("PWD Received");
                    outStream.println(currentDirectory.getPath());
                    break;
                case "DIR":
                    System.out.println("DIR Received");
                    outStream.println(getDirectory(currentDirectory.getPath()));
                    break;
                case "CD":
                    System.out.println("CD Received");
                    currentDirectory = changeDirectory(rawInput.split("\\s+")[1],
                            currentDirectory, outStream);
                    outStream.println(currentDirectory.getPath());

                    break;
                default:
                    System.out.println("Client's input is invalid.");
            }
        }

        // Close the client connection when finished.
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File changeDirectory(String clientInput,
                                 File currentDirectory,
                                 PrintWriter outStream) {
        // outStream.println(directory);

        File newFilePath = null;

        //Go up a level
        if (clientInput.equals("..")) {
            newFilePath = new File(currentDirectory.getParent());
        }


        return newFilePath;

        // System.out.println(File.separator + " " + File.pathSeparator);

    }

    private String getDirectory(String directory) {
        File folder = new File(directory);
        File[] directoryListing = folder.listFiles();
        System.out.println("There are " + directory.length() + " items in the" +
                " directory.");

        StringBuilder output = new StringBuilder();

        // Build a new string with files at the top and directories at the
        // bottom. Replace all newline characters with the '/' char because
        // linux and windows file names cannot contain it. This allows us to use
        // println(). Otherwise the multiple newlines would break the string
        // apart at the newline char when reading it with readLine() on the
        // client.
        if (directoryListing != null) {
            for (File file : directoryListing) {
                if (file.isFile()) {
                    output.insert(0, "File" + "         "  + file.getName() +
                            "/" );
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

