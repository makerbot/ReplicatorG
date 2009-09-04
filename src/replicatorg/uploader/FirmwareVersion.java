package replicatorg.uploader;

import org.w3c.dom.Node;

public class FirmwareVersion {
	public int major;
	public int minor;
	public String where;
	public FirmwareVersion(Node n) {
		major = Integer.parseInt(n.getAttributes().getNamedItem("major").getNodeValue());
		minor = Integer.parseInt(n.getAttributes().getNamedItem("minor").getNodeValue());
		where = n.getAttributes().getNamedItem("relpath").getNodeValue();
	}
	public String toString() {
		return "v" + Integer.toString(major) + "." + Integer.toString(minor);
	}
}
