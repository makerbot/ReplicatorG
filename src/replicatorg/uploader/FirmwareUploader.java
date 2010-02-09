package replicatorg.uploader;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import replicatorg.app.Base;
import replicatorg.uploader.ui.UploaderDialog;

public class FirmwareUploader {
	/**
	 * The constructor is non-public; only one firmware uploader is permitted to be running at
	 * any given time.  To start the uploader, use "startUploader".
	 * @param parent A parent widget, used to construct the interfaces, etc.
	 */
	private FirmwareUploader(Frame parent)
	{
		this.parent = parent;
	}
	
	private Frame parent; 
	
	/**
	 * Start the uploader.  Synchronized to ensure only one uploader is running at a time.
	 * @param parent A parent widget, used to construct the user interfaces.
	 */
	public static synchronized void startUploader(Frame parent) {
		FirmwareUploader uploader = new FirmwareUploader(parent);
		uploader.run();
	}
	
	public void run() {
		// Load firmware.xml
		UploaderDialog selector = new UploaderDialog(parent,this);
		selector.setVisible(true);
		// wait for dialog to close
	}
	
	Document firmwareDoc = null;
	
	/**
	 * Initiate check for new firmware.  Lock on class.
	 */
	public static synchronized void checkFirmware() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				FirmwareRetriever retriever = new FirmwareRetriever(getFirmwareFile(),getFirmwareURL());
				System.err.println(retriever.checkForUpdates().toString());
			}
		});
		t.start();
	}
	
	static public final String DEFAULT_UPDATES_URL = "http://firmware.makerbot.com/firmware.xml";
	/**
	 * Get the URL of the source for dowloading 
	 */
	protected static URL getFirmwareURL() {
		try {
			String url = Base.preferences.get("replicatorg.updates.url", DEFAULT_UPDATES_URL);
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
		File f = new File("firmware.xml");
		if (!f.exists()) {
			File alternate = new File("firmware.xml.dist");
			if (alternate.exists()) return alternate;
		}
		return f;
	}
	
	public Document getFirmwareDoc() {
		if (firmwareDoc == null) { firmwareDoc = loadFirmwareDoc(); }
		return firmwareDoc;
	}
		
	public static Document loadFirmwareDoc() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			try {
				File f = getFirmwareFile();
				if (!f.exists()) {
					Base.showError(
							"Firmware.xml Not Found",
							"The firmware description file 'firmware.xml' was not found.\n" +
							"Make sure you're running ReplicatorG from the correct directory.",
							null);
					return null;
				}
				try {
					 doc = db.parse(f);
				} catch (SAXException e) {
					Base.showError("Parse error",
							"Error parsing firmware.xml.  You may need to reinstall ReplicatorG.",
							e);
					return null;
				}
			} catch (IOException e) {
				Base.showError(null, "Could not read firmware.xml.\n"
						+ "You may need to reinstall ReplicatorG.", e);
				return null;
			}
		} catch (ParserConfigurationException e) {
			Base.showError("Unkown error", "Unknown error parsing firmware.xml.", e);
			return null;
		}
		return doc;
	}

}
