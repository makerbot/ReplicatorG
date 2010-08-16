package fabman.coordinator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The FabCoordinator is a server process that provides descriptions of available fabrication
 * targets on a given machine.  FabCoordinator is responsible for monitoring and serving up
 * currently running fabrication targets, describing possible fabrication targets, and starting
 * processes for fabrication targets that are not yet running.
 * 
 * FabCoordinators listen on the standard fabricator coordinator port, 4499.
 * 
 * FabCoordinators should not be directly instantiated or accessed by client code.
 * Instead, client code should utilize the FabManager class, which will manage connections to
 * FabCoordinators and instantiate them if necessary.
 * 
 * @author phooky
 *
 */
public class FabCoordinator implements Runnable {
	// For now, let's pick something from http://www.iana.org/assignments/port-numbers
	// that is marked as Unassigned.  4499 is easy to remember.
	private static final int DEFAULT_COORDINATOR_PORT = 4499;
	
	// The maximum amount of time that a thread should wait on a given socket before giving
	// up on the transaction and closing the socket.  Currently set to two minutes.
	private static final int TIMEOUT_MS = 120 * 1000;
	
	ServerSocket listenSocket;
	boolean running;
	
	public FabCoordinator() throws IOException {
		init(DEFAULT_COORDINATOR_PORT);
	}

	public FabCoordinator(int portNumber) throws IOException {
		init(portNumber);
	}

	private void init(int portNumber) throws IOException {
		listenSocket = new ServerSocket(portNumber);
		threadPool = Executors.newFixedThreadPool(5);
	}

	ExecutorService threadPool;
	
	public void run() {
		running = true;
		while (running) {
			try {
				Socket incomingSocket = listenSocket.accept();
				incomingSocket.setSoTimeout(TIMEOUT_MS);
				threadPool.execute(new FabCoordinatorThread(incomingSocket));
			} catch (SocketException e) {
				// Socket may have been closed; if so, terminate.
				running = !listenSocket.isClosed();
			} catch (IOException e) {
				// TODO: log exception
				e.printStackTrace();
			}
		}
	}
	
	public void shutdown() throws IOException {
		listenSocket.close();
		threadPool.shutdown();
	}
}
