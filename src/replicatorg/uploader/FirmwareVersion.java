package replicatorg.uploader;

import java.io.File;

import org.w3c.dom.Node;

public class FirmwareVersion {
	private int major;
	private int minor;
	private String name;
	private String where;
	
	private String getAttrNodeValue(Node n, String attr) {
		Node n2 = n.getAttributes().getNamedItem(attr);
		if (n2 == null) return null;
		return n2.getNodeValue();
	}
	
	public FirmwareVersion(Node n) {
		// Required attributes
		major = Integer.parseInt(getAttrNodeValue(n,"major"));
		minor = Integer.parseInt(getAttrNodeValue(n,"minor"));
		// default name
		name = getAttrNodeValue(n,"name");
		if (name == null) name = "v" + Integer.toString(major) + "." + Integer.toString(minor); 
		where = getAttrNodeValue(n,"relpath");
	}
	
	public String getName() { return name; }
	
	public int getMajor() { return major; }
	public int getMinor() { return minor; }
	
	public String getRelPath() { return where; }

	public File getFile() { return new File(where); }
	
	public String toString() {
		return "v" + Integer.toString(major) + "." + Integer.toString(minor);
	}
}
