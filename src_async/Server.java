import java.io.Console;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

// TODO: remove this class because no need it anymore
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
        this.fileSystemHandler = new FileSystemHandler(System.getProperty("user.dir"));

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

    private boolean isTransferCmd(String rawCmd) {
        return rawCmd.startsWith("LIST") || rawCmd.startsWith("STOR") ||
                rawCmd.startsWith("APPE") || rawCmd.startsWith("RETR");
    }

    private void run() {
        int noOfConnections = 0;
        Map<String, String> clientProperties = new HashMap<String, String>();
        clientProperties.put(channelType, clientChannel);

        Map<SocketChannel, ConnectionHandler> connections = new HashMap<SocketChannel, ConnectionHandler>();
        Map<SocketChannel, Map<TransferCommandHandler, CompletableFuture<String>>> transferCommandsMap = new HashMap<SocketChannel, Map<TransferCommandHandler, CompletableFuture<String>>>();
        Map<SocketChannel, TransferCommandHandler> tmpTransferCommandsMap = new HashMap<SocketChannel, TransferCommandHandler>();

        // ExecutorService executor = Executors.newFixedThreadPool(100);
        ExecutorService executor = Executors.newCachedThreadPool();
        String blockType = "select";

        while (true) {
            try {
                int selectResult = 0;

                if (connections.size() == 0) {
                    blockType = "select";
                } else {
                    blockType = "selectNow";
                }

                if (blockType.equals("select")) {
                    selectResult = this.selector.select();
                } else {
                    selectResult = this.selector.selectNow();
                }

                if (selectResult != 0) {
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

                                clientKey.attach(clientProperties);
                                int dataPort = this.controlPort + noOfConnections + 1;
                                ConnectionHandler handler = new ConnectionHandler(clientSocketChannel, this.database,
                                        this.fileSystemHandler, dataPort);
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
                                    System.out.println(cmd);

                                    if (cmd.startsWith("PORT")) {
                                        tmpTransferCommandsMap.put(clientChannel,
                                                new TransferCommandHandler(clientChannel, cmd.substring(5),
                                                        this.fileSystemHandler));
                                    } else if (this.isTransferCmd(cmd)) {
                                        TransferCommandHandler transferCommandHandler = tmpTransferCommandsMap
                                                .get(clientChannel);
                                        if (!transferCommandsMap.containsKey(clientChannel)) {
                                            transferCommandsMap.put(clientChannel,
                                                    new HashMap<TransferCommandHandler, CompletableFuture<String>>());
                                        }

                                        CompletableFuture<String> future = transferCommandHandler.executeCommand(cmd,
                                                executor);
                                        transferCommandsMap.get(clientChannel).put(transferCommandHandler, future);
                                        tmpTransferCommandsMap.remove(clientChannel);
                                    } else {
                                        connections.get(clientChannel).executeCommand(cmd);
                                    }
                                }
                            }
                        }

                        iterator.remove();
                    }
                }

                if (connections.size() != 0) {
                    Iterator<Map.Entry<SocketChannel, ConnectionHandler>> outerIter = connections.entrySet().iterator();
                    while (outerIter.hasNext()) {
                        Map.Entry<SocketChannel, ConnectionHandler> entry = outerIter
                                .next();
                        boolean allDone = true;
                        /* check if all futures are done */
                        if (transferCommandsMap.containsKey(entry.getKey())) {
                            Iterator<Map.Entry<TransferCommandHandler, CompletableFuture<String>>> innerIter = transferCommandsMap
                                    .get(entry.getKey()).entrySet().iterator();
                            while (innerIter.hasNext()) {
                                Map.Entry<TransferCommandHandler, CompletableFuture<String>> item = innerIter.next();
                                if (!item.getValue().isDone()) {
                                    allDone = false;
                                } else {
                                    if (item.getValue() != null) {
                                        try {
                                            item.getKey().closeTransfer(item.getValue().get());
                                        } catch (Exception e) {
                                        }
                                    } else {
                                        item.getKey().closeTransfer(null);
                                    }

                                    innerIter.remove();
                                }
                            }
                        }

                        if (allDone && entry.getValue().isClosed()) {
                            entry.getKey().close();
                            transferCommandsMap.remove(entry.getKey());
                            outerIter.remove();
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception encountered on selection");
                e.printStackTrace();
            }
        }
    }
};
