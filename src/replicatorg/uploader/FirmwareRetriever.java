package replicatorg.uploader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;

/**
 * Update firmware list from the website's canonical list and download any new releases.
 * @author phooky
 *
 */
class FirmwareRetriever {
	File firmwareXml;
	URL firmwareSourceURL;

	public FirmwareRetriever(File firmwareXml, URL firmwareSourceURL) {
		this.firmwareXml = firmwareXml;
		this.firmwareSourceURL = firmwareSourceURL;
	}

	public enum UpdateStatus {
		NETWORK_UNAVAILABLE,
		NO_NEW_UPDATES,
		NEW_UPDATES,
		RO_FILESYSTEM // Can't save the downloaded firmware because running in a read-only filesystem.
	}

	/// Use a 10-second timeout when checking
	final private static int TIMEOUT_MS = 10 * 1000;
	/**
	 * Check our server
	 * @return an UpdateStatus reflecting the result of the update check.
	 */
	UpdateStatus checkForUpdates() {
		Base.logger.fine("Firmware Source URL : "+ firmwareSourceURL);
		UpdateStatus status;
		synchronized(getClass()) {
			status = updateURL(firmwareSourceURL,firmwareXml,true);
			if (status == UpdateStatus.NEW_UPDATES) {
				// Pull down any new firmware that we haven't seen before.
				retrieveNewFirmware();
			}
		}
		return status;
	}

	protected UpdateStatus updateURL(URL url, File file) {
		return updateURL(url,file,false);
	}

	protected UpdateStatus updateURL(URL url, File file, boolean checkParseableXML) {
		long timestamp = file.lastModified();
		if (checkParseableXML) {
			// Check the xml file for parseability.  If it is unparseable, set the timestamp
			// to 0 to force it to pull a new version from the server.
			if (file.exists()) {
				try {
					DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					db.parse(file);
				} catch (Exception e) {
					Base.logger.severe("Existing "+file.getPath()+" is not parseable; forcing refresh from web.");
					timestamp = 0;
				}
			}
		}
		try {
			URLConnection urlConnection = url.openConnection();

			// If this is an HTTP url, check if it has been updated (otherwise, we always assume it has been updated)
			if ((urlConnection instanceof HttpURLConnection)) {
				HttpURLConnection connection = (HttpURLConnection)urlConnection;
				connection.setIfModifiedSince(timestamp);
				connection.setConnectTimeout(TIMEOUT_MS);
				connection.connect();
				if (connection instanceof HttpURLConnection) {
					int rc = ((HttpURLConnection)connection).getResponseCode();
					if ((rc == HttpURLConnection.HTTP_NOT_MODIFIED) ||
						(connection.getIfModifiedSince() > connection.getLastModified())) {
						return UpdateStatus.NO_NEW_UPDATES;
					}
					if (rc != HttpURLConnection.HTTP_OK) {
						// Do not attempt to pull down the file if the connection failed.
						return UpdateStatus.NETWORK_UNAVAILABLE;
					}
				}
			}

			// Pull down the file.  The content should be an input stream.
                        //Get content doesn't work if the "Content-Type: text/plain" header is not present in the HTTP header.
                        //Some servers don't know how exactly to treat .hex files and so it's safe to assume that this will not be around in most packet responses.
			//Therefore, we must use the raw input stream piped to a buffered stream for getting our data

			BufferedReader packetData = new BufferedReader( new InputStreamReader( urlConnection.getInputStream() ) );
			FileWriter fileOutput = new FileWriter( file.getAbsoluteFile() );
			BufferedWriter fileBufferedOutput = new BufferedWriter(fileOutput);

			// Welcome to 1994!  Seriously, there's no standard util for this?  Lame.

			int bytesWritten = 0;
			final int BUF_SIZE=2048;
			char buf[] = new char[BUF_SIZE];
			
			while (true) {
				int count = packetData.read(buf,0,BUF_SIZE);
				if (count == -1) break;
				bytesWritten = bytesWritten + count;
				fileBufferedOutput.write(buf, 0, count);
			}
			packetData.close();

			fileBufferedOutput.close();
			fileOutput.close();

			Base.logger.info(Integer.toString(bytesWritten) + " bytes written to "+file.getCanonicalPath());
			return UpdateStatus.NEW_UPDATES;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketTimeoutException e) {
			Base.logger.log(Level.INFO,"Firmware updater: network unavailable.");
			// Fine; network is unavailable
		} catch (UnknownHostException e) {
			Base.logger.log(Level.INFO,"Firmware updater: network unavailable.");
			// Fine; network is unavailable
		} catch (FileNotFoundException e) {
			// This ordinarily indicates that the user is running on a read-only filesystem
			// and we've got nowhere to save the incoming firmware.
			return UpdateStatus.RO_FILESYSTEM;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return UpdateStatus.NETWORK_UNAVAILABLE;
	}

	protected void retrieveNewFirmware() {
		Document doc = FirmwareUploader.loadFirmwareDoc();
		if (doc == null) { return; }
		NodeList list = doc.getElementsByTagName("firmware");
		for (int i = 0; i < list.getLength(); i++) {
			Node n = list.item(i);
			String path = new FirmwareVersion(n).getRelPath();
			URL url;
			try {
				url = new URL(firmwareSourceURL,path);
				Base.logger.fine("Checking remote file: "+ url);
				File file = Base.getUserFile(path);
				updateURL(url,file);
			} catch (MalformedURLException e) {
				Base.logger.severe("Couldn't generate URL for path "+path);
			}
			String eeprom = new FirmwareVersion(n).getEepromPath();
			if (eeprom != null) try {
				url = new URL(firmwareSourceURL,eeprom);
				Base.logger.fine("Checking remote file: "+ url);
				File file = Base.getUserFile(path);
				updateURL(url,file);
			} catch (MalformedURLException e) {
				Base.logger.severe("Couldn't generate URL for path "+path);
			}

		}
	}
}
