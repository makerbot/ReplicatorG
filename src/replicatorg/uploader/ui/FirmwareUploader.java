package replicatorg.uploader.ui;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import replicatorg.app.Base;

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
	
	public Document getFirmwareDoc() {
		if (firmwareDoc != null) { return firmwareDoc; }
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			try {
				File f = new File("firmware.xml");
				if (!f.exists()) {
					f = new File("firmware.xml.dist");
					if (!f.exists()) {
						Base.showError(
								"Firmware.xml Not Found",
								"The firmware description file 'firmware.xml' was not found.\n"
								+ "Make sure you're running ReplicatorG from the correct directory.",
								null);
						return null;
					}
				}
				try {
					 firmwareDoc = db.parse(f);
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
		return firmwareDoc;
	}

}
