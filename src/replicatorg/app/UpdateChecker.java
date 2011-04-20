package replicatorg.app;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Hit the ReplicatorG website to see if there's a new release.
 * @author phooky
 *
 */
public class UpdateChecker {
	static public final String DEFAULT_UPDATES_URL = "http://download.replicat.org/updates.xml";

	protected static URL getUpdateURL() {
		try {
			String url = Base.preferences.get("replicatorg.app.updates.url",
					DEFAULT_UPDATES_URL);
			return new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/// Use a 10-second timeout when checking
	final private static int TIMEOUT_MS = 10 * 1000;

	public static void checkLatestVersion(final Component parent) {
		Thread t = new Thread("ReplicatorG Version Checker") {
			public void run() {
				doCheckVersion(parent);
			}
		};
		t.start();
	}
	
	private static void doCheckVersion(final Component parent) {
		final int latestVersion = getLatestVersion();
		final String lvString = Integer.toString(latestVersion);
		if (latestVersion > Base.VERSION) {
			final String key = "replicatorG.ignoreNewVersion." + lvString;
			if (Base.preferences.getBoolean(key, false)) {
				return; // We've been told to ignore this message
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JCheckBox checkbox = new JCheckBox(
					"Do not show this message for this version again");
					String message = "A newer version ("
						+ lvString
						+ ") of ReplicatorG is now available.\n"
						+ "Would youlike to visit the download page?";
					Object[] params = { message, checkbox };
					int result = JOptionPane.showOptionDialog(parent, params,
							"New Version Available", JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, null, null);
					boolean dontShow = checkbox.isSelected();
					Base.preferences.putBoolean(key, dontShow);
					if (result == JOptionPane.YES_OPTION) {
						Base.openURL("http://replicat.org/download/");
					}
				}
			});
	    }
	}
	/**
	 * Check our server for new versions
	 * @return an UpdateStatus reflecting the result of the update check.
	 */
	private static int getLatestVersion() {
		int latestVersion = Base.VERSION;
		URL url = getUpdateURL();
		if (url == null) {
			Base.logger.warning("Couldn't construct update URL!");
			return latestVersion;
		}
		try {
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setConnectTimeout(TIMEOUT_MS);
			try {
				connection.connect();
			} catch (SocketTimeoutException ste) {
				Base.logger.log(Level.INFO,"Network unavailable or update site timed out.");
				return latestVersion;
			} catch (UnknownHostException uhe) {
				Base.logger.log(Level.INFO,"Network unavailable or update URL incorrect.");
				return latestVersion;				
			}
		    InputStream inputStream = (InputStream)connection.getContent();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(inputStream);
				NodeList nl = doc.getElementsByTagName("release");
				for (int i = 0; i < nl.getLength(); i++) {
					String v = nl.item(i).getAttributes().getNamedItem("version").getNodeValue();
					int releaseVersion = Integer.parseInt(v);
					if (releaseVersion > latestVersion) {
						latestVersion = releaseVersion;
					}
				}			
			} catch (SAXException e) {
				Base.logger.log(Level.INFO,
						"Parse error in version file at "+url.toExternalForm(),
						e);
			} catch (ParserConfigurationException e) {
				Base.logger.log(Level.WARNING,"Couldn't construct parser",e);
			}
		} catch (IOException e) {
			Base.logger.log(Level.WARNING,
					"IOException retrieving version file at "+url.toExternalForm(),
					e);
		}
		return latestVersion;
	}	
}