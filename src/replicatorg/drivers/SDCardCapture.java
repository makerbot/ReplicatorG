package replicatorg.drivers;

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
	
	public ResponseCode playback(String filename);

	/**
	 * Returns whether SD card capture capabilities are supported by the current machine.
	 * @return true if this version of the firmware supports SDCardCapture.
	 */
	boolean hasFeatureSDCardCapture();
}
