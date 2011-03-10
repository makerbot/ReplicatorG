package replicatorg.drivers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface SDCardCapture {
	
	enum ResponseCode {
		SUCCESS,
		FAIL_NO_CARD,
		FAIL_INIT,
		FAIL_PARTITION,
		FAIL_FS,
		FAIL_ROOT_DIR,
		FAIL_LOCKED,
		FAIL_NO_FILE,
		FAIL_GENERIC,
	}
	
	public ResponseCode beginCapture(String filename);
	/**
	 * 
	 * @return The number of bytes captured, or -1 on error.
	 */
	public int endCapture();
	
	public void beginFileCapture(String path) throws FileNotFoundException;
	public void endFileCapture() throws IOException;

	public ResponseCode playback(String filename);

	/**
	 * Returns whether SD card capture capabilities are supported by the current machine.
	 * @return true if this version of the firmware supports SDCardCapture.
	 */
	boolean hasFeatureSDCardCapture();
	
	/**
	 * Return a list of the file paths of printable files.
	 */
	List<String> getFileList();
	
	/** 
	 * True if a playback is finished.
	 * TODO: Why does this cover playback in the first place??
	 */
	public boolean isFinished();
}
