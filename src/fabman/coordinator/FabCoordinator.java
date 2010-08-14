package fabman.coordinator;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class FabCoordinator {
	// For now, let's pick something from http://www.iana.org/assignments/port-numbers
	// that is marked as Unassigned.  4499 is easy to remember.
	static final int DEFAULT_COORDINATOR_PORT = 4499;
	ServerSocket listenSocket;
	boolean running;
	
	public FabCoordinator() throws IOException {
		init(DEFAULT_COORDINATOR_PORT);
	}
	
	private void init(int portNumber) throws IOException {
		listenSocket = new ServerSocket(portNumber);
	}
	
	public void run() {
		running = true;
		while (running) {
			try {
				Socket incomingSocket = listenSocket.accept();
				if (incomingSocket != null) { handleRequest(incomingSocket); }
			} catch (SocketException e) {
				// Socket may have been closed; if so, terminate.
				running = !listenSocket.isClosed();
			} catch (IOException e) {
				// TODO: log exception
				e.printStackTrace();
			}
		}
	}
	
	void handleRequest(Socket socket) throws IOException {
		InputStream is = socket.getInputStream();	
	}
}
