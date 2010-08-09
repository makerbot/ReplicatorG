package fabman.manager;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A FabDescriptor is a description of a fabricator.  The fabricator may
 * be real or virtual.  It may represent an open connection to an instantiated
 * fabricator (such as a ReplicatorG simulator, or an actual running Makerbot),
 * or a connection which may be instantiated (a Makerbot that is not currently
 * connected, or a write-to-file virtual fabricator).
 * 
 * FabDescriptors may also contain a reasonable amount of information about the
 * device itself, such as the build area, whether the device is real or virtual,
 * whether the fabricator is currently connected, and so on.  It also provides
 * an (optional) copy of the XML that describes the machine, so the application
 * can extract arbitrary details from the descriptor.
 * 
 * @author phooky
 *
 */
public class FabDescriptor {
	Element xmlDescription = null;
	String name;
	
	/**
	 * Generate a fabricator descriptor using an Machine element.
	 * @param xmlSource The element node of the XML document representing the fab.
	 */
	public FabDescriptor(Element xmlSource) {
		xmlDescription = (Element)xmlSource.cloneNode(true);
		NodeList names = xmlDescription.getElementsByTagName("name");
		assert names.getLength() == 1;
		name = names.item(0).getTextContent();
		assert name != null && !name.isEmpty();
	}
	
	/**
	 * Get the human-readable name of this descriptor.
	 * @return A non-null string name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Retrieve the XML descriptor that was used to generate this object, if any.
	 * @return An Element containing the top-level Machine element, or null if 
	 *         no XML data is available.
	 */
	Element getXmlDescriptor() {
		return xmlDescription;
	}
	
}
