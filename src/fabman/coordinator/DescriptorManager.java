package fabman.coordinator;

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

import fabman.manager.FabDescriptor;

/**
 * A DescriptorManager object manages loading and updating the fabrication descriptors.
 * @author phooky
 *
 */
class DescriptorManager {
	
	private static final String DEFAULT_DESCRIPTOR_PATH = "~/.fabman/machines"; 

	private Collection<FabDescriptor> descriptors;

	public DescriptorManager(Collection<File> descriptorPaths) {
		if (descriptorPaths == null) {
			descriptorPaths = getDefaultDescriptorPaths();
		}
		descriptors = new Vector<FabDescriptor>();
		for (File file : descriptorPaths) {
			addDescriptorsFromFile(file);
		}

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


	private synchronized void addDescriptorsFromFile(File file) {
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

	/* ---------- Implementation of coordinator functions --------- */
	public synchronized Collection<FabDescriptor> getFabDescriptorList() {
		return descriptors;
	}

}
