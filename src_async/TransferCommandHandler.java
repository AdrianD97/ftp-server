import java.net.Socket;
import java.io.PrintWriter;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TransferCommandHandler {
    private enum transferType {
        ASCII, BINARY
    }

    private Socket dataConnection;
    private PrintWriter dataOutWriter;
    private SocketChannel clientChannel;
    private static transferType transferMode = transferType.ASCII;

    private String ip;
    private int port;
    private Debug debug;

    /**
     * keep reference to the file system handler instance
     */
    private FileSystemHandler fileSystemHandler;

    public TransferCommandHandler(SocketChannel clientChannel, String rawArgs, FileSystemHandler fileSystemHandler) {
        String[] stringSplit = rawArgs.split(",");
        this.ip = stringSplit[0] + "." + stringSplit[1] + "." + stringSplit[2] + "." + stringSplit[3];

        this.port = Integer.parseInt(stringSplit[4]) * 256 + Integer.parseInt(stringSplit[5]);
        this.debug = new Debug(true);
        this.clientChannel = clientChannel;
        this.fileSystemHandler = fileSystemHandler;

        this._initConnection();
        this._sendMsgToClient("200 Command OK");
    }

    private void _initConnection() {
        try {
            this.dataConnection = new Socket(this.ip, this.port);
            this.dataOutWriter = new PrintWriter(this.dataConnection.getOutputStream(), true);
            this.debug.out("Data connection - Active Mode - established");
        } catch (IOException e) {
            this.debug.out("Could not connect to client data socket");
            e.printStackTrace();
        }
    }

    /**
     * Main command dispatcher method. Separates the command from the arguments and
     * dispatches it to single handler functions.
     * 
     * @param cmd the raw input from the socket consisting of command and arguments
     */
    public CompletableFuture executeCommand(String cmd, ExecutorService executor) {
        /* split command and arguments */
        int index = cmd.indexOf(' ');
        String command = ((index == -1) ? cmd.toUpperCase() : (cmd.substring(0, index)).toUpperCase());
        String args = ((index == -1) ? null : cmd.substring(index + 1));
        CompletableFuture future = null;

        this.debug.out("Command: " + command + " Args: " + args);

        /* dispatcher mechanism for different commands */
        switch (command) {
            case "LIST":
                future = this._handleNlst(args, executor);
                break;

            case "RETR":
                future = this._handleRetr(args, executor);
                break;

            case "STOR":
                future = this._handleStor(args, executor);
                break;

            case "APPE":
                future = this._handleAppe(args, executor);
                break;
        }

        return future;
    }

    public void closeTransfer(String result) {
        if (result != null) {
            this._sendMsgToClient(result);
        }

        this._closeDataConnection();
    }

    /**
     * Close established data connection sockets and streams
     */
    private void _closeDataConnection() {
        try {
            this.dataOutWriter.close();
            this.dataConnection.close();
            this.debug.out("Data connection was closed");
        } catch (IOException e) {
            this.debug.out("Could not close data connection");
            e.printStackTrace();
        }
    }

    private void _sendMsgToClient(String msg) {
        try {
            CharBuffer buffer = CharBuffer.wrap(msg + "\n");
            while (buffer.hasRemaining()) {
                this.clientChannel.write(Charset.defaultCharset()
                        .encode(buffer));
            }
            buffer.clear();
        } catch (IOException e) {
            System.out.println("Exception encountered on sending message to client");
            e.printStackTrace();
        }
    }

    /**
     * Handler for NLST (Named List) command. Lists the directory content in a short
     * format (names only)
     * 
     * @param path     The directory to be listed
     * @param executor Executor service used for running async operations
     */
    private CompletableFuture _handleNlst(String path, ExecutorService executor) {
        if (this.dataConnection == null || this.dataConnection.isClosed()) {
            this._sendMsgToClient("425 No data connection was established");
            this.debug.out("Cannot send message, because no data connection is established");
        } else {
            String[] dirContent = this.fileSystemHandler.ls(path);

            if (dirContent == null) {
                this._sendMsgToClient("550 File does not exist.");
            } else {
                this._sendMsgToClient("125 Opening ASCII mode data connection for file list.");

                return CompletableFuture.supplyAsync(() -> {
                    for (int i = 0; i < dirContent.length; ++i) {
                        this.dataOutWriter.print(dirContent[i] + '\r' + '\n');
                    }

                    return "226 Transfer complete.";
                }, executor);
            }
        }

        return null;
    }

    /**
     * Handler for the RETR (retrieve) command. Retrieve transfers a file from the
     * ftp server to the client.
     * 
     * @param path     The path to the file to transfer to the user (path also
     *                 contains
     *                 the name of the file)
     * @param executor Executor service used for running async
     *                 operations
     */
    private CompletableFuture _handleRetr(String path, ExecutorService executor) {
        if (path == null) {
            this._sendMsgToClient("501 No path given");
            return null;
        }

        int lastIndex = path.lastIndexOf('/');
        String name = path.substring(lastIndex + 1);
        OutputStream tmpStream = null;

        try {
            tmpStream = this.dataConnection.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            this._sendMsgToClient("535 Failed to download file " + name);
            return null;
        }

        final OutputStream stream = tmpStream;
        CompletableFuture future = null;

        /* Binary mode */
        if (transferMode == transferType.BINARY) {
            this._sendMsgToClient("150 Opening binary mode data connection for requested file " + name);

            this.debug.out("Starting file transmission of " + name);

            future = CompletableFuture.supplyAsync(() -> {
                if (this.fileSystemHandler.download(path, FileType.BINARY,
                        new BufferedOutputStream(stream)) == false) {
                    return "535 Failed to download file " + name;
                } else {
                    this.debug.out("Completed file transmission of " + name);

                    return "226 File transfer successful. Closing data connection.";
                }
            }, executor);
        } else {
            /* ASCII mode */
            this._sendMsgToClient("150 Opening ASCII mode data connection for requested file " + name);

            this.debug.out("Starting file transmission of " + name);

            future = CompletableFuture.supplyAsync(() -> {
                if (this.fileSystemHandler.download(path, FileType.ASCII,
                        new PrintWriter(stream, true)) == false) {
                    return "535 Failed to download file " + name;
                } else {
                    this.debug.out("Completed file transmission of " + name);

                    return "226 File transfer successful. Closing data connection.";
                }
            }, executor);
        }

        return future;
    }

    /**
     * Handler for STOR (Store) command. Store receives a file from the client and
     * saves it to the ftp server.
     * 
     * @param path     File path (the path also contains the name of the file)
     * 
     * @param executor Executor service used for running async
     *                 operations
     */
    private CompletableFuture _handleStor(String path, ExecutorService executor) {
        return this._handleFile(path, false, executor);
    }

    /**
     * Handler for APPE (Append) command. Append receives a file from the client and
     * append to it the data sent by client on open connection.
     * 
     * @param path     File path (the path also contains the name of the file)
     * @param executor Executor service used for running async
     *                 operations
     */
    private CompletableFuture _handleAppe(String path, ExecutorService executor) {
        return this._handleFile(path, true, executor);
    }

    /**
     * used to write a new file or to append to a file (if the file not found
     * a new file is created)
     * 
     * @param path   File path (the path also contains the name of the file)
     * @param append true if it has to append to a file; false to create a new file
     */
    private CompletableFuture _handleFile(String path, boolean append, ExecutorService executor) {
        if (path == null) {
            this._sendMsgToClient("501 No path given");
            return null;
        }

        int lastIndex = path.lastIndexOf('/');
        String name = path.substring(lastIndex + 1);
        InputStream tmpStream = null;

        try {
            tmpStream = dataConnection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            this._sendMsgToClient("532 Failed to " + (append == true ? "append to file " : "upload file ") + name);
            return null;
        }

        final InputStream stream = tmpStream;

        this._sendMsgToClient("150 Opening " + (transferMode == transferType.BINARY ? "binary" : "ASCII")
                + " mode data connection for requested file " + name);

        this.debug.out("Start " + (append == true ? "appending to file " : "storing file ") + name);

        return CompletableFuture.supplyAsync(() -> {
            boolean result;

            /* Binary mode */
            if (transferMode == transferType.BINARY) {
                result = this.fileSystemHandler.upload(path.substring(0, lastIndex), name, FileType.BINARY,
                        new BufferedInputStream(stream), append);
            } else {
                /* ASCII mode */
                result = this.fileSystemHandler.upload(path.substring(0, lastIndex), name, FileType.ASCII,
                        new BufferedReader(new InputStreamReader(stream)), append);
            }

            if (result == false) {
                return "532 Failed " + (append == true ? "appending to file " : "storing file ") + name;
            } else {
                this.debug.out("Completed " + (append == true ? "appending to file " : "storing file ") + name);

                return "226 File transfer successful. Closing data connection.";
            }
        }, executor);
    }
}
