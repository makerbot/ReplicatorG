package replicatorg.uploader;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import replicatorg.app.Base;
import replicatorg.drivers.Version;
import replicatorg.uploader.ui.UploaderDialog;

public class FirmwareUploader {
	/**
	 * The constructor is non-public; only one firmware uploader is permitted to
	 * be running at any given time. To start the uploader, use "startUploader".
	 * 
	 * @param parent
	 *            A parent widget, used to construct the interfaces, etc.
	 */
	private FirmwareUploader(Frame parent) {
		this.parent = parent;
	}

	private Frame parent;

	private static FirmwareUploader uploader = null;

	/**
	 * Start the uploader. Synchronized to ensure only one uploader is running
	 * at a time.
	 * 
	 * @param parent
	 *            A parent widget, used to construct the user interfaces.
	 */
	public static void startUploader(Frame parent) {
		synchronized (FirmwareUploader.class) {
			if (uploader != null) {
				return;
			}
			uploader = new FirmwareUploader(parent);
		}
		uploader.run();
	}

	public void run() {
		// Load firmware.xml
		UploaderDialog selector = new UploaderDialog(parent, this);
		selector.setVisible(true);
		// wait for dialog to close
		synchronized(FirmwareUploader.class) {
			uploader = null;
		}
	}

	static Document firmwareDoc = null;

	/**
	 * Initiate check for new firmware.
	 */
	public static void checkFirmware() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				
				FirmwareRetriever retriever = new FirmwareRetriever(
						getFirmwareFile(), getFirmwareURL());
				FirmwareRetriever.UpdateStatus status = retriever.checkForUpdates();
				Base.logger.fine("Firmware retriever status: "+status.name());
			}
		}, "Firmware Update Checker");
		t.start();
	}

	static public final String DEFAULT_UPDATES_URL = "http://firmware.makerbot.com/firmware.xml";

	/**
	 * Get the URL of the source for dowloading
	 */
	protected static URL getFirmwareURL() {
		try {
			String url = Base.preferences.get("replicatorg.updates.url",
					DEFAULT_UPDATES_URL);
			return new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the path of the XML file describing the available firmware.
	 */
	protected static File getFirmwareFile() {
		File f = Base.getUserFile("firmware.xml");
		if (!f.exists()) {
			File alternate = Base.getApplicationFile("firmware.xml.dist");
			if (alternate.exists())
				return alternate;
		}
		return f;
	}

	public static Document getFirmwareDoc() {
		if (firmwareDoc == null) {
			firmwareDoc = loadFirmwareDoc();
		}
		return firmwareDoc;
	}

	public static void invalidateFirmware() {
		firmwareDoc = null;
		File f = Base.getUserFile("firmware.xml");
		if (f.exists()) { f.delete(); } // Blow away the old firmware file
	}
	
	// / Check the latest version. Returns true if the user wants to update the
	// firmware and
	// / abort the connections.
	public static boolean checkLatestVersion(final String boardName, Version version) {
		final Version latest = getLatestVersion(boardName);
		if (latest == null)
			return false;
		if (latest.compareTo(version) > 0) {
			Base.logger.info("latest " + latest.toString() + " old "
					+ version.toString());
			final String key = "replicatorG.ignoreFirmware." + boardName + "."
					+ version.toString();
			if (Base.preferences.getBoolean(key, false)) {
				return false;
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JCheckBox checkbox = new JCheckBox(
					"Do not show this message for this version again");
					String message = "A newer version ("
						+ latest.toString()
						+ ") of the "
						+ boardName
						+ " firmware is now available.\n"
						+ "Use the \"Upload Firmware...\" item in the \"Machine\" menu to upload it to your machine.";
					Object[] params = { message, checkbox };
					JOptionPane.showMessageDialog(null, params,
							"New Firmware Available", JOptionPane.INFORMATION_MESSAGE);
					boolean dontShow = checkbox.isSelected();
					Base.preferences.putBoolean(key, dontShow);
				}	
			});
			return true;
		}
		return false;
	}

	// Return the latest available version for the given board name
	public static Version getLatestVersion(String boardName) {
		Document firmwareDoc = getFirmwareDoc();
		if (firmwareDoc == null) return null;
		NodeList nl = firmwareDoc.getElementsByTagName("board");
		Version version = null;
		for (int i = 0; i < nl.getLength(); i++) {
			String name = nl.item(i).getAttributes().getNamedItem("name")
					.getNodeValue();
			if (name.equalsIgnoreCase(boardName)) {
				NodeList children = nl.item(i).getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {
					Node n = children.item(j);
					if ("firmware".equalsIgnoreCase(n.getNodeName())) {
						int major = Integer.parseInt(n.getAttributes()
								.getNamedItem("major").getNodeValue());
						int minor = Integer.parseInt(n.getAttributes()
								.getNamedItem("minor").getNodeValue());
						Version candidate = new Version(major, minor);
						if (version == null) {
							version = candidate;
						} else if (candidate.compareTo(version) > 0) {
							version = candidate;
						}
					}
				}
				break;
			}
		}
		return version;
	}

	public static Document loadFirmwareDoc() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {	
			DocumentBuilder db = dbf.newDocumentBuilder();
			synchronized (FirmwareRetriever.class) {
				try {
					File f = getFirmwareFile();
					if (!f.exists()) {
						Base.showWarning(
							"Firmware.xml Not Found",
							"The firmware description file 'firmware.xml' was not found.\n" +
							"You may see this message if you're running ReplicatorG for the\n" +
							"first time and are not connected to the internet, or if you are\n" +
							"running ReplicatorG on a read-only filesystem.",
							null);
						return null;
					}
					try {
						doc = db.parse(f);
					} catch (SAXException e) {
						Base.showWarning("Parse error",
							"Error parsing firmware.xml. \n"+
							"You may see this message if you're running ReplicatorG for the\n" +
							"first time and are not connected to the internet, or if you are\n" +
							"running ReplicatorG on a read-only filesystem.",
							e);
						return null;
					}
				} catch (IOException e) {
					Base.showWarning(null, "Could not read firmware.xml.\n" +
							"You may see this message if you're running ReplicatorG for the\n" +
							"first time and are not connected to the internet, or if you are\n" +
							"running ReplicatorG on a read-only filesystem.",
							e);
					return null;
				}
			}
		} catch (ParserConfigurationException e) {
			Base.showWarning("Unkown error", "Unknown error parsing firmware.xml.", e);
			return null;
		}
		return doc;
	}
}
