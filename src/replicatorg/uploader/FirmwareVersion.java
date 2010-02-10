package replicatorg.uploader;

import java.io.File;

import org.w3c.dom.Node;

import replicatorg.drivers.Version;

public class FirmwareVersion {
	private Version version;
	private String name;
	private String where;
	
	private String getAttrNodeValue(Node n, String attr) {
		Node n2 = n.getAttributes().getNamedItem(attr);
		if (n2 == null) return null;
		return n2.getNodeValue();
	}
	
	public FirmwareVersion(Node n) {
		// Required attributes
		int major = Integer.parseInt(getAttrNodeValue(n,"major"));
		int minor = Integer.parseInt(getAttrNodeValue(n,"minor"));
		version = new Version(major, minor);
		// default name
		name = getAttrNodeValue(n,"name");
		if (name == null) name = "v" + version.toString(); 
		where = getAttrNodeValue(n,"relpath");
	}
	
	public String getName() { return name; }
	
	public Version getVersion() { return version; }
	
	public String getRelPath() { return where; }

	public File getFile() { return new File(where); }
	
	public String toString() {
		return name;
	}
}
