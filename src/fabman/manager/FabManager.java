package fabman.manager;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * FabManager is a top-level fab connection manager.  FabManager is responsible for discovering
 * connected fabrication queues, enumerating connectable queues, and starting servers for queues
 * that are connectable but not yet running.
 * 
 * FabManager should ordinarily be obtained by using the static factory call getFabManager().
 * Other factory calls exist for testing purposes only.
 * 
 * Concurrency: all calls need to assert a global mutex to avoid multiple instances attempting to
 * create connections against the same device simultaneously.
 * 
 * @author phooky
 *
 */
public class FabManager {

	/* ---------- Static Factory Calls ---------- */
	
	/**
	 * Retrieve a default FabManager.
	 * @return a valid FabManager object.
	 */
	public static FabManager getFabManager() {
		return getFabManager(getDefaultDescriptorPaths());
	}
	
	/**
	 * Retrieve a FabManager that searches the given array of descriptor paths and
	 * files to discover available devices.  
	 * @param descriptorPaths
	 * @return a FabManager object
	 */
	public static FabManager getFabManager(Collection<File> descriptorPaths) {
		return new FabManager(descriptorPaths);
	}
	
	/**
	 * Generate a vector of files representing the default paths to search for
	 * valid machine XML descriptors.
	 * @return a collection of path to search.
	 */
	private static Collection<File> getDefaultDescriptorPaths() {
		Collection<File> paths = new Vector<File>();
		paths.add(new File("~/.fabman/machines"));
		return paths;
	}

	/* ----------- Members ----------- */
	private Collection<FabDescriptor> descriptors;
	private DocumentBuilder db = null;
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
				if (db == null) {
					db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				}
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
	
	/* ----------- Constructors and Initialization ----------- */
	private FabManager(Collection<File> paths) {
		descriptors = new Vector<FabDescriptor>();
		for (File file : paths) {
			addDescriptorsFromFile(file);
		}
	}
	
	/* ----------- Descriptor Collection ----------- */
	public Collection<FabDescriptor> getAvailableDescriptors() {
		return descriptors;
	}
	
	/* ----------- Open Fab ----------- */
	public FabConnection openFab(FabDescriptor descriptor) {
		return null;
	}
}
