import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * FTP Server class. On receiving a new connection it creates a
 * new worker thread.
 */
public class Server {
	private final int controlPort = 1025;
	private ServerSocket welcomeSocket;
	private Database database = null;
	private FileSystem fileSystem = null;

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
		this.fileSystem = new FileSystem(System.getProperty("user.dir"), root);

		if (this.fileSystem.init() == false) {
			System.out.println("Could not init file system");
			System.exit(-1);
		}

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
			this.welcomeSocket = new ServerSocket(controlPort);
		} catch (IOException e) {
			System.out.println("Could not create server socket");
			System.exit(-1);
		}

		System.out.println("FTP Server started listening on port " + controlPort);
	}

	private void run() {
		int noOfThreads = 0;

		while (true) {

			try {
				Socket client = this.welcomeSocket.accept();

				/**
				 * Port for incoming dataConnection (for passive mode) is
				 * the controlPort + number of created threads + 1
				 */
				int dataPort = controlPort + noOfThreads + 1;

				/* Create new worker thread for new connection */
				Worker w = new Worker(client, this.database, this.fileSystem, dataPort);

				System.out.println("New connection received. Worker was created.");
				noOfThreads++;
				w.start();
			} catch (IOException e) {
				System.out.println("Exception encountered on accept");
				e.printStackTrace();
			}
		}
	}
};
