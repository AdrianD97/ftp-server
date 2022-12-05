import java.net.Socket;
import java.io.PrintWriter;

public class TransferCommandHandler {
    private Socket dataConnection;
    private PrintWriter dataOutWriter;

    public TransferCommandHandler(String rawArgs) {
        /**
         * TODO:
         * - parse raw args => get ip and port (what about not storing ip and address)
         * - open data connection
         */
        String[] stringSplit = args.split(",");
        String hostName = stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3];

        int p = Integer.parseInt(stringSplit[4]) * 256 + Integer.parseInt(stringSplit[5]);

        this._initConnection(hostName, p);
    }

    private void _initConnection(String ipAddress, int port) {
        try {
            dataConnection = new Socket(ipAddress, port);
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            Utility.debugOutput("Data connection - Active Mode - established");
        } catch (IOException e) {
            Utility.debugOutput("Could not connect to client data socket");
            e.printStackTrace();
        }
    }

    public void 
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
     * TODO: very important: need to get db and file ystem instance
     */
}
