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
}
