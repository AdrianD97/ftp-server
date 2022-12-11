import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Class responsible for handling connections
 */
public class ConnectionHandler {
    /**
     * Enable debugging output to console
     */
    private boolean debugMode = true;

    private boolean closed = false;

    /**
     * Indicating the last set transfer type
     */
    private enum transferType {
        ASCII, BINARY
    }

    /**
     * Indicates the authentification status of a user
     */
    private enum userStatus {
        NOTLOGGEDIN, ENTEREDUSERNAME, LOGGEDIN
    }

    /**
     * keep reference to the database used by the FTP server
     * to authenticate all connections
     */
    private Database database;

    /**
     * keep reference to the file system handler instance
     */
    private FileSystemHandler fileSystemHandler;

    // Path information
    private String currDirectory;
    private String fileSeparator = "/";

    // control connection
    private SocketChannel controlSocket;
    private PrintWriter controlOutWriter;
    private BufferedReader controlIn;

    // data Connection
    private ServerSocket dataSocket;
    private Socket dataConnection;
    private PrintWriter dataOutWriter;

    private int dataPort;
    private transferType transferMode = transferType.ASCII;

    // user properly logged in?
    private userStatus currentUserStatus = userStatus.NOTLOGGEDIN;
    private User user = null;

    /**
     * Create new worker with given client socket
     * 
     * @param dataPort the port for the data connection
     */
    public ConnectionHandler(SocketChannel clientChannel, Database database, FileSystemHandler fileSystemHandler,
            int dataPort) {
        this.controlSocket = clientChannel;
        this.database = database;
        this.fileSystemHandler = fileSystemHandler;
        this.dataPort = dataPort;
        this.currDirectory = System.getProperty("user.dir") + "/test";

        sendMsgToClient("220");
    }

    /**
     * Main command dispatcher method. Separates the command from the arguments and
     * dispatches it to single handler functions.
     * 
     * @param cmd the raw input from the socket consisting of command and arguments
     */
    public boolean executeCommand(String cmd) {
        /* split command and arguments */
        int index = cmd.indexOf(' ');
        String command = ((index == -1) ? cmd.toUpperCase() : (cmd.substring(0, index)).toUpperCase());
        String args = ((index == -1) ? null : cmd.substring(index + 1));

        debugOutput("Command: " + command + " Args: " + args);

        /* dispatcher mechanism for different commands */
        switch (command) {
            case "USER":
                handleUser(args);
                break;

            case "PASS":
                handlePass(args);
                break;

            case "PWD":
            case "XPWD":
                handlePwd();
                break;

            case "QUIT":
                handleQuit();
                break;

            case "PASV":
                handlePasv();
                break;

            case "EPSV":
                handleEpsv();
                break;

            case "SYST":
                handleSyst();
                break;

            case "FEAT":
                handleFeat();
                break;

            case "PORT":
                handlePort(args);
                break;

            case "EPRT":
                handleEPort(args);
                break;

            case "MKD":
            case "XMKD":
                handleMkd(args);
                break;

            case "RMD":
            case "XRMD":
                handleRmd(args);
                break;

            case "TYPE":
                handleType(args);
                break;

            case "DELE":
                handleDele(args);
                break;

            case "RNFR":
                // handleRnfr(args);
                sendMsgToClient("350");
                break;

            default:
                sendMsgToClient("501 Unknown command");
                break;
        }

        return true;
    }

    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Sends a message to the connected client over the control connection. Flushing
     * is automatically performed by the stream.
     * 
     * @param msg The message that will be sent
     */
    private void sendMsgToClient(String msg) {
        try {
            CharBuffer buffer = CharBuffer.wrap(msg + "\n");
            while (buffer.hasRemaining()) {
                this.controlSocket.write(Charset.defaultCharset()
                        .encode(buffer));
            }
            buffer.clear();
        } catch (IOException e) {
            System.out.println("Exception encountered on sending message to client");
            e.printStackTrace();
        }
    }

    /**
     * Open a new data connection socket and wait for new incoming connection from
     * client. Used for passive mode.
     * 
     * @param port Port on which to listen for new incoming connection
     */
    private void openDataConnectionPassive(int port) {
        try {
            dataSocket = new ServerSocket(port);
            dataConnection = dataSocket.accept();
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            debugOutput("Data connection - Passive Mode - established");

        } catch (IOException e) {
            debugOutput("Could not create data connection.");
            e.printStackTrace();
        }

    }

    /**
     * Connect to client socket for data connection. Used for active mode.
     * 
     * @param ipAddress Client IP address to connect to
     * @param port      Client port to connect to
     */
    private void openDataConnectionActive(String ipAddress, int port) {
        try {
            dataConnection = new Socket(ipAddress, port);
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            debugOutput("Data connection - Active Mode - established");
        } catch (IOException e) {
            debugOutput("Could not connect to client data socket");
            e.printStackTrace();
        }

    }

    /**
     * Close previously established data connection sockets and streams
     */
    private void closeDataConnection() {
        try {
            dataOutWriter.close();
            dataConnection.close();
            if (dataSocket != null) {
                dataSocket.close();
            }

            debugOutput("Data connection was closed");
        } catch (IOException e) {
            debugOutput("Could not close data connection");
            e.printStackTrace();
        }
        dataOutWriter = null;
        // dataConnection = null;
        dataSocket = null;
    }

    /**
     * Handler for USER command. User identifies the client.
     * 
     * @param username Username entered by the user
     */
    private void handleUser(String username) {
        if (currentUserStatus == userStatus.NOTLOGGEDIN) {
            this.user = this.database.getUsersColection().findUserByUsername(username);

            if (this.user == null) {
                sendMsgToClient("530 Need account for login");
                return;
            }

            sendMsgToClient("331 User name okay, need password");
            currentUserStatus = userStatus.ENTEREDUSERNAME;
        } else if (currentUserStatus == userStatus.LOGGEDIN) {
            sendMsgToClient("530 User already logged in");
        } else {
            sendMsgToClient("530 Not logged in");
        }
    }

    /**
     * Handler for PASS command. PASS receives the user password and checks if it's
     * valid.
     * 
     * @param password Password entered by the user
     */
    private void handlePass(String password) {
        /* User has entered a valid username and password is correct */
        if (currentUserStatus == userStatus.ENTEREDUSERNAME && password.equals(this.user.password)) {
            currentUserStatus = userStatus.LOGGEDIN;
            sendMsgToClient("230 User logged in successfully");
        } else if (currentUserStatus == userStatus.LOGGEDIN) {
            /* User is already logged in */
            sendMsgToClient("530 User already logged in");
        } else { /* Wrong password */
            sendMsgToClient("530 Not logged in");
        }
    }

    /**
     * Handler for the PORT command. The client issues a PORT command to the server
     * in active mode, so the server can open a data connection to the client
     * through the given address and port number.
     * 
     * @param args The first four segments (separated by comma) are the IP address.
     *             The last two segments encode the port number (port = seg1*256 +
     *             seg2)
     */
    private void handlePort(String args) {
        // Extract IP address and port number from arguments
        String[] stringSplit = args.split(",");
        String hostName = stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3];

        int p = Integer.parseInt(stringSplit[4]) * 256 + Integer.parseInt(stringSplit[5]);

        // Initiate data connection to client
        openDataConnectionActive(hostName, p);
        sendMsgToClient("200 Command OK");
    }

    /**
     * Handler for the EPORT command. The client issues an EPORT command to the
     * server in active mode, so the server can open a data connection to the client
     * through the given address and port number.
     * 
     * @param args This string is separated by vertical bars and encodes the IP
     *             version, the IP address and the port number
     */
    private void handleEPort(String args) {
        final String IPV4 = "1";
        final String IPV6 = "2";

        // Example arg: |2|::1|58770| or |1|132.235.1.2|6275|
        String[] splitArgs = args.split("\\|");
        String ipVersion = splitArgs[1];
        String ipAddress = splitArgs[2];

        if (!IPV4.equals(ipVersion) && !IPV6.equals(ipVersion)) {
            throw new IllegalArgumentException("Unsupported IP version");
        }

        int port = Integer.parseInt(splitArgs[3]);

        // Initiate data connection to client
        openDataConnectionActive(ipAddress, port);
        sendMsgToClient("200 Command OK");
    }

    /**
     * Handler for PWD (Print working directory) command. Returns the path of the
     * current directory back to the client.
     */
    private void handlePwd() {
        sendMsgToClient("257 \"" + currDirectory + "\"");
    }

    /**
     * Handler for PASV command which initiates the passive mode. In passive mode
     * the client initiates the data connection to the server. In active mode the
     * server initiates the data connection to the client.
     */
    private void handlePasv() {
        // Using fixed IP for connections on the same machine
        // For usage on separate hosts, we'd need to get the local IP address from
        // somewhere
        // Java sockets did not offer a good method for this
        String myIp = "127.0.0.1";
        String myIpSplit[] = myIp.split("\\.");

        int p1 = dataPort / 256;
        int p2 = dataPort % 256;

        sendMsgToClient("227 Entering Passive Mode (" + myIpSplit[0] + "," + myIpSplit[1] + "," + myIpSplit[2] + ","
                + myIpSplit[3] + "," + p1 + "," + p2 + ")");

        openDataConnectionPassive(dataPort);
    }

    /**
     * Handler for EPSV command which initiates extended passive mode. Similar to
     * PASV but for newer clients (IPv6 support is possible but not implemented
     * here).
     */
    private void handleEpsv() {
        sendMsgToClient("229 Entering Extended Passive Mode (|||" + dataPort + "|)");
        openDataConnectionPassive(dataPort);
    }

    /**
     * Handler for the QUIT command.
     */
    private void handleQuit() {
        sendMsgToClient("221 Closing connection");
        this.closed = true;
    }

    private void handleSyst() {
        sendMsgToClient("215 " + System.getProperty("os.name"));
    }

    /**
     * Handler for the FEAT (features) command. Feat transmits the
     * abilities/features of the server to the client. Needed for some ftp clients.
     * This is just a dummy message to satisfy clients, no real feature information
     * included.
     */
    private void handleFeat() {
        sendMsgToClient("211-Extensions supported:");
        sendMsgToClient("211 END");
    }

    /**
     * Handler for the MKD (make directory) command. Creates a new directory on the
     * server.
     * 
     * @param path Directory path (the path also contains the name of the directory)
     */
    private void handleMkd(String path) {
        int lastIndex = path.lastIndexOf('/');
        String name = path.substring(lastIndex + 1);

        /* Allow only alphanumeric characters */
        if (name != null && name.matches("^[a-zA-Z0-9]+$")) {
            if (this.fileSystemHandler.mkdir(path.substring(0, lastIndex), name) == false) {
                sendMsgToClient("550 Failed to create new directory");
                debugOutput("Failed to create new directory");
            } else {
                sendMsgToClient("250 Directory successfully created");
            }
        } else {
            sendMsgToClient("550 Invalid name");
        }

    }

    /**
     * Handler for RMD (remove directory) command. Removes a directory.
     * 
     * @param dir directory to be deleted.
     */
    private void handleRmd(String dir) {
        String filename = currDirectory;

        // only alphanumeric folder names are allowed
        if (dir != null && dir.matches("^[a-zA-Z0-9]+$")) {
            filename = filename + fileSeparator + dir;

            // check if file exists, is directory
            File d = new File(filename);

            if (d.exists() && d.isDirectory()) {
                d.delete();

                sendMsgToClient("250 Directory was successfully removed");
            } else {
                sendMsgToClient("550 Requested action not taken. File unavailable.");
            }
        } else {
            sendMsgToClient("550 Invalid file name.");
        }

    }

    /**
     * Handler for the TYPE command. The type command sets the transfer mode to
     * either binary or ascii mode
     * 
     * @param mode Transfer mode: "a" for Ascii. "i" for image/binary.
     */
    private void handleType(String mode) {
        if (mode.toUpperCase().equals("A")) {
            transferMode = transferType.ASCII;
            sendMsgToClient("200 OK");
        } else if (mode.toUpperCase().equals("I")) {
            transferMode = transferType.BINARY;
            sendMsgToClient("200 OK");
        } else {
            sendMsgToClient("504 Not OK");
        }
    }

    /**
     * Handler for DELE (delete a file) command.
     * 
     * @param path File path (the path also contains the name of the file)
     */
    private void handleDele(String path) {
        if (path == null) {
            sendMsgToClient("501 No path given");
            return;
        }

        int lastIndex = path.lastIndexOf('/');
        String name = path.substring(lastIndex + 1);

        if (this.fileSystemHandler.rm(path) == false) {
            sendMsgToClient("532 Failed to remove file " + name);
        } else {
            sendMsgToClient("250 Successfully deleted file " + name);
        }
    }

    /**
     * Debug output to the console. Also includes the Thread ID for better
     * readability.
     * 
     * @param msg Debug message
     */
    private void debugOutput(String msg) {
        if (debugMode) {
            System.out.println(msg);
        }
    }
}
