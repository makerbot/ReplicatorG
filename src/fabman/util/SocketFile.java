package fabman.util;

import java.io.File;
import java.net.Socket;

/**
 * 
 * Every process ordinarily has its own socket file, which it cleans up upon close.
 * When attempting to establish a connection, the ordinary procedure is to find the
 * appropriate socket file.  If the socket file does not exist, it is presumed that
 * the process needs to be started.  If the socket encoded in the file does not
 * exist, the process is presumed to be dead and the file should be cleaned up.
 * 
 * @author phooky
 *
 */
public class SocketFile {
	private static File tempDir = null;
	private static File getTempDir() {
		if (tempDir == null) {
			String tempDirPath = System.getProperty("java.io.tmpdir");
			tempDir = new File(tempDirPath);
		}
		return tempDir;
	}
	
	private File file;
	private Socket socket;
	
	public SocketFile(String filename, Socket socket) {
		file = new File(getTempDir(),filename);
	}
	
}
