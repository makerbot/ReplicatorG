package fabman.manager;

import javax.media.j3d.BoundingBox;
import javax.vecmath.Point3d;

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
	BoundingBox envelope = null;
	
	/**
	 * Load the build envelope bounds from the geometry element.
	 */
	private BoundingBox boundsFromElement(Element e) {
		Point3d min = new Point3d();
		Point3d max = new Point3d();
		NodeList axes = e.getElementsByTagName("axis");
		for (int idx = 0; idx < axes.getLength(); idx++) {
			Element axis = (Element)axes.item(idx);
			String idStr = axis.getAttribute("id");
			if (idStr == null || idStr.isEmpty()) { continue; }
			String minStr = axis.getAttribute("min");
			String maxStr = axis.getAttribute("max");
			double minValue = Double.MIN_VALUE;
			double maxValue = Double.MAX_VALUE;
			if (minStr != null && !minStr.isEmpty()) { minValue = Double.parseDouble(minStr); }
			if (maxStr != null && !maxStr.isEmpty()) { maxValue = Double.parseDouble(maxStr); }
			if ("x".equals(idStr)) { min.x = minValue; max.x = maxValue; }
			else if ("y".equals(idStr)) { min.y = minValue; max.y = maxValue; }
			else if ("z".equals(idStr)) { min.z = minValue; max.z = maxValue; }
		}
		return new BoundingBox(min,max);
	}
	
	/**
	 * Generate a fabricator descriptor using an Machine element.
	 * @param xmlSource The element node of the XML document representing the fab.
	 */
	public FabDescriptor(Element xmlSource) {
		xmlDescription = (Element)xmlSource.cloneNode(true);
		name = xmlDescription.getAttribute("name");
		assert name != null && !name.isEmpty();
		NodeList geometries = xmlDescription.getElementsByTagName("geometry");
		if (geometries.getLength() > 0) {
			envelope = boundsFromElement((Element)geometries.item(0));
		}
	}
	
	/**
	 * Get the human-readable name of this descriptor.
	 * @return A non-null string name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the physical bounds of this printing device's build envelope.
	 * @return a Bounds object, or null if no bounds are specified for the device.
	 */
	public BoundingBox getBuildEnvelope() {
		return envelope;
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
