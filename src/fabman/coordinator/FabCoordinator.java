package fabman.coordinator;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fabman.manager.FabDescriptor;

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
	
	private static final String DEFAULT_DESCRIPTOR_PATH = "~/.fabman/machines"; 
		
	ServerSocket listenSocket;
	boolean running;
	
	public FabCoordinator() throws IOException {
		init(DEFAULT_COORDINATOR_PORT, null);
	}

	public FabCoordinator(int portNumber) throws IOException {
		init(portNumber, null);
	}

	/**
	 * Generate a vector of files representing the default paths to search for
	 * valid machine XML descriptors.
	 * @return a collection of path to search.
	 */
	private static Collection<File> getDefaultDescriptorPaths() {
		Collection<File> paths = new Vector<File>();
		paths.add(new File(DEFAULT_DESCRIPTOR_PATH));
		return paths;
	}

	private Collection<FabDescriptor> descriptors;

	private void addDescriptorsFromFile(File file) {
		if (!file.exists()) { return; } // Ignore non-existent paths
		if (file.isDirectory()) {
			// Recursively scan directory
			File[] listing = file.listFiles();
			for (File child : listing) {
				addDescriptorsFromFile(child);
			}
		} else {
			// Add nodes from file
			try {
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = db.parse(file);
				NodeList machines = doc.getElementsByTagName("machine");
				for (int idx = 0; idx < machines.getLength(); idx++) {
					descriptors.add(new FabDescriptor((Element)machines.item(idx)));
				}
			} catch (ParserConfigurationException e) {
				// TODO: error log
			} catch (SAXException e) {
				// TODO: error log
			} catch (IOException e) {
				// TODO: error log
			}

		}
	}

	private void init(int portNumber, Collection<File> descriptorPaths) throws IOException {
		listenSocket = new ServerSocket(portNumber);
		threadPool = Executors.newFixedThreadPool(5);
		if (descriptorPaths == null) {
			descriptorPaths = getDefaultDescriptorPaths();
		}
		descriptors = new Vector<FabDescriptor>();
		for (File file : descriptorPaths) {
			addDescriptorsFromFile(file);
		}
	}

	private ExecutorService threadPool;
	
	public void run() {
		running = true;
		while (running) {
			try {
				Socket incomingSocket = listenSocket.accept();
				incomingSocket.setSoTimeout(TIMEOUT_MS);
				threadPool.execute(new FabCoordinatorThread(this,incomingSocket));
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
