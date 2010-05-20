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
import java.net.UnknownHostException;
import java.util.logging.Level;

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
		//System.err.println("PATH : "+ firmwareSourceURL.getPath());
		UpdateStatus status;
		synchronized(getClass()) {
			status = updateURL(firmwareSourceURL,firmwareXml);
			if (status == UpdateStatus.NEW_UPDATES) {
				// Pull down any new firmware that we haven't seen before.
				retrieveNewFirmware();
			}
		}
		return status;
	}

	protected UpdateStatus updateURL(URL url, File file) {
		long timestamp = file.lastModified();
		try {
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setIfModifiedSince(timestamp);
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.connect();
			if (connection instanceof HttpURLConnection) {
				int rc = ((HttpURLConnection)connection).getResponseCode();
				if (rc == HttpURLConnection.HTTP_NOT_MODIFIED) {
					return UpdateStatus.NO_NEW_UPDATES;
				}
			}
			// Pull down the file.  The content should be an input stream.
			InputStream content = (InputStream)connection.getContent();
			FileOutputStream out = new FileOutputStream(file);
			// Welcome to 1994!  Seriously, there's no standard util for this?  Lame.
			final int BUF_SIZE=2048; 
			byte buf[] = new byte[BUF_SIZE];
			int bytesWritten = 0;
			while (true) {
				int count = content.read(buf);
				if (count == -1) break;
				bytesWritten = bytesWritten + count;
				out.write(buf, 0, count);
			}
			out.close();
			content.close();
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
		NodeList list = doc.getElementsByTagName("firmware");
		for (int i = 0; i < list.getLength(); i++) {
			Node n = list.item(i);
			String path = new FirmwareVersion(n).getRelPath();
			URL url;
			try {
				url = new URL(firmwareSourceURL,path);
				File file = new File(path);
				updateURL(url,file);
			} catch (MalformedURLException e) {
				Base.logger.severe("Couldn't generate URL for path "+path);
			}
		}
	}
}
