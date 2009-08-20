package replicatorg.drivers;

public interface SDCardCapture {
	public void beginCapture(String filename);
	/**
	 * 
	 * @return The number of bytes captured, or -1 on error.
	 */
	public int endCapture();
	
	public void playback(String filename);
}
