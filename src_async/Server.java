import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

final class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
};

/**
 * TODO:
 * you need to keep all connections on the main thread
 * what must you store for each connection?
 * - the main socket
 * - user data (as username and credentials -> need them for authentication)
 * - for each list, put, get and append command you get a PORT command
 * - the ideea is to keep each commandhandler (let's call it task and it must be
 * responsible for handling
 * these tasks: list, upload, downlod)
 * - create a task each time when a PORT command is received and store it in the
 * hashmap (how do you clean)
 * let's crete a TransferCommandHandler
 * it will handle put, get, list, upload commands -> for each command you need a
 * new data connection to be open
 * 
 * when get exit command, just remove the entry from hashmap, because the
 * command will be closed
 * after the trasnmission is done :))
 * 
 * the user connection class will be resposnible for user authenticathion
 * and for each PORT received command will also receive the transfer command
 */

/**
 * FTP Server class - async version
 */

public class Server {
    private static String clientChannel = "clientChannel";
    private static String serverChannel = "serverChannel";
    private static String channelType = "channelType";

    private final int controlPort = 1025;
    private final String host = "localhost";
    private ServerSocketChannel channel;
    private Selector selector;
    private Database database = null;
    private FileSystemHandler fileSystemHandler = null;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Invalid number of arguments");
            System.out.println("Run: java server <root folder name>");
            System.exit(-1);
        }

        new Server(args[0]).run();
    }

    public Server(String root) {
        /* init file system */
        this.fileSystemHandler = new FileSystemHandler(System.getProperty("user.dir"));// + "/" + root);

        /* load database */
        try {
            this.database = new Database("./db/database.json");
            this.database.load();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not load database");
            System.exit(-1);
        }

        /* create main socket the server can accept connection on */
        try {
            this.channel = ServerSocketChannel.open();
            this.channel.bind(new InetSocketAddress(this.host, this.controlPort));
            this.channel.configureBlocking(false);
            this.selector = Selector.open();

            SelectionKey socketServerSelectionKey = this.channel.register(selector, SelectionKey.OP_ACCEPT);
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(channelType, serverChannel);
            socketServerSelectionKey.attach(properties);
        } catch (IOException e) {
            System.out.println("Could not create server socket");
            System.exit(-1);
        }

        System.out.println("FTP Server started listening on port " + controlPort);
    }

    private void run() {
        int noOfConnections = 0;
        Map<String, String> clientproperties = new HashMap<String, String>();
        clientproperties.put(channelType, clientChannel);

        Map<SocketChannel, ConnectionHandler> connections = new HashMap<SocketChannel, ConnectionHandler>();
        // ExecutorService executor = Executors.newFixedThreadPool(100);
        ExecutorService executor = Executors.newCachedThreadPool();

        while (true) {
            try {
                if (this.selector.select() == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    if (((Map) key.attachment()).get(channelType).equals(serverChannel)) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientSocketChannel = serverSocketChannel.accept();

                        if (clientSocketChannel != null) {
                            clientSocketChannel.configureBlocking(false);
                            SelectionKey clientKey = clientSocketChannel.register(
                                    selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);

                            clientKey.attach(clientproperties);
                            int dataPort = this.controlPort + noOfConnections + 1;
                            ConnectionHandler handler = new ConnectionHandler(clientSocketChannel, this.database,
                                    this.fileSystemHandler, dataPort, noOfConnections);
                            connections.put(clientSocketChannel, handler);
                            System.out.println("New connection received. Connection handler created successfully.");
                            noOfConnections++;
                        }
                    } else {
                        ByteBuffer buffer = ByteBuffer.allocate(100);
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        int bytesRead = 0;

                        if (key.isReadable()) {
                            if ((bytesRead = clientChannel.read(buffer)) > 0) {
                                buffer.flip();
                                String cmd = Charset.defaultCharset().decode(buffer).toString().trim();
                                buffer.clear();

                                /**
                                 * TODO:
                                 * if quit command, then call the QUIT handler from main thread
                                 * you could end-up with connection to be closed before handling QUIT command
                                 */

                                if (cmd == "QUIT") {
                                    connections.get(clientChannel).executeCommand(cmd);
                                } else {
                                    CompletableFuture.supplyAsync(() -> {
                                        return connections.get(clientChannel).executeCommand(cmd);
                                    }, executor);
                                }
                            } else if (bytesRead < 0) {
                                clientChannel.close();
                                connections.remove(clientChannel);
                                System.out
                                        .println("noOfConnections = " + noOfConnections + " -> " + "connections.size = "
                                                + connections.size());
                            }
                        }
                    }

                    iterator.remove();
                }
            } catch (IOException e) {
                System.out.println("Exception encountered on selection");
                e.printStackTrace();
            }
        }
    }
};
