package replicatorg.drivers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import replicatorg.app.Base;

public interface SDCardCapture {
	
	enum ResponseCode {
		SUCCESS("Success!"),
		FAIL_NO_CARD(
				"No SD card was detected.  Please make sure you have a working, formatted\n" +
				"SD card in the motherboard's SD slot and try again."),
		FAIL_INIT(
				"ReplicatorG was unable to initialize the SD card.  Please make sure that\n" +
				"the SD card works properly."),
		FAIL_PARTITION(
				"ReplicatorG was unable to read the SD card's partition table.  Please check\n" +
				"that the card is partitioned properly.\n" +
				"If you believe your SD card is OK, try resetting your device and restarting\n" +
				"ReplicatorG."),
		FAIL_FS(
				"ReplicatorG was unable to open the filesystem on the SD card.  Please make sure\n" +
				"that the SD card has a single partition formatted with a FAT16 filesystem."),
		FAIL_ROOT_DIR(
				"ReplicatorG was unable to read the root directory on the SD card.  Please\n"+
				"check to see if the SD card was formatted properly."),
		FAIL_LOCKED(
				"The SD card cannot be written to because it is locked.  Remove the card,\n" +
				"switch the lock off, and try again."),
		FAIL_NO_FILE(
				"ReplicatorG could not find the build file on the SD card."),
		FAIL_GENERIC("Unknown SD card error.");
		
		private String message;
		
		private ResponseCode(String message)
		{
			this.message = message;
		}
		
		/**
		 * Process an SD response code and throw up an appropriate dialog for the user.
		 * @param code the response from the SD request
		 * @return true if the code indicates success; false if the operation should be aborted
		 */
		public static boolean processSDResponse(SDCardCapture.ResponseCode code) {
			if (code == SDCardCapture.ResponseCode.SUCCESS) return true;
			String message = code.message;
			Base.logger.log(Level.WARNING, message);
			JOptionPane.showMessageDialog(
					null,
					message,
					"SD card error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
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
