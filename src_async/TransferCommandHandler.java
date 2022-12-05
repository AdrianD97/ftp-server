import java.net.Socket;
import java.io.PrintWriter;

public class TransferCommandHandler {
    private Socket dataConnection;
    private PrintWriter dataOutWriter;

    private String ip;
    private int port;
    private Debug debug;

    /**
     * keep reference to the file system handler instance
     */
    private FileSystemHandler fileSystemHandler;

    public TransferCommandHandler(String rawArgs, FileSystemHandler fileSystemHandler) {
        /**
         * TODO:
         * - parse raw args => get ip and port (what about not storing ip and address)
         * - open data connection
         */
        String[] stringSplit = args.split(",");
        this.ip = stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3];

        this.port = Integer.parseInt(stringSplit[4]) * 256 + Integer.parseInt(stringSplit[5]);
        this.debug = new Debug(true);
    }

    private void _initConnection() {
        try {
            dataConnection = new Socket(this.ip, this.port);
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            debug.out("Data connection - Active Mode - established");
        } catch (IOException e) {
            debug.out("Could not connect to client data socket");
            e.printStackTrace();
        }
    }

    /**
     * Main command dispatcher method. Separates the command from the arguments and
     * dispatches it to single handler functions.
     * 
     * @param cmd the raw input from the socket consisting of command and arguments
     */
    public void executeCommand(String cmd) {
        /* split command and arguments */
        int index = cmd.indexOf(' ');
        String command = ((index == -1) ? cmd.toUpperCase() : (cmd.substring(0, index)).toUpperCase());
        String args = ((index == -1) ? null : cmd.substring(index + 1));

        debug.out("Command: " + command + " Args: " + args);

        /* init connection */
        this._initConnection();

        /* dispatcher mechanism for different commands */
        switch (command) {
            case "LIST":
                handleNlst(args);
                break;
        }

        /* close connection */
    }

    /**
     * Handler for NLST (Named List) command. Lists the directory content in a short
     * format (names only)
     * 
     * @param path The directory to be listed
     */
    private void handleNlst(String path) {
        if (dataConnection == null || dataConnection.isClosed()) {
            sendMsgToClient("425 No data connection was established");
        } else {
            String[] dirContent = this.fileSystemHandler.ls(path);

            if (dirContent == null) {
                sendMsgToClient("550 File does not exist.");
            } else {
                sendMsgToClient("125 Opening ASCII mode data connection for file list.");

                for (int i = 0; i < dirContent.length; ++i) {
                    sendDataMsgToClient(dirContent[i]);
                }

                sendMsgToClient("226 Transfer complete.");
            }

        }
    }
    /**
     * TODO:
     * what should this class be responsible for?
     * - handle PORT command and get ip address and port
     * - open a connection; establishing this connacetion will allow us to
     * communicate with client
     * - handle each of the following commands properly: append, upload, downoad,
     * listing
     * 
     * OBS:
     * - when the main thread gets a PORT command, it should create a
     * TransferCommandHandler and add keep it into a hashtable
     * - using this approach we avoid the overriding of data connection
     * 
     * VERY IMPORTANT NOTE:
     * - remove the entry from the hastable as soon as you get the
     * transfer command => in this way you avoid synchronization issues
     */

    /**
     * - should have an execute command method
     * for each transfer command will create async job (each job will open
     * a data connection do the task, and in the end will close the connection)
     * - it will avoid the overriding
     */

    /**
     * take in account the following:
     * - each object must handle a single transfer command
     * - it simplifies things a lot => expose a execute command method that
     * handle raw commands
     */
    /**
     * TODO: very important: need to get file system instance
     */
}
