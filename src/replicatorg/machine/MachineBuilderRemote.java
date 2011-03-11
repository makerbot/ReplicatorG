package replicatorg.machine;

import java.util.EnumMap;
import java.util.Map;

import javax.swing.JOptionPane;

import replicatorg.drivers.SDCardCapture;


/** Helper class to build on a machine from a remote file.
 * 
 * @author mattmets
 *
 */
public class MachineBuilderRemote implements MachineBuilder{

	SDCardCapture driver;
	
	boolean finished = false;
	
	public MachineBuilderRemote(SDCardCapture driver, String remoteName) {
		if (!processSDResponse(driver.playback(remoteName))) {
			finished = true;
		}
	}
	
	@Override
	public boolean finished() {
		return (driver.isFinished());
	}

	@Override
	public void runNext() {
	}
	
	
	// TODO: Where does this go?
	static Map<SDCardCapture.ResponseCode,String> sdErrorMap =
		new EnumMap<SDCardCapture.ResponseCode,String>(SDCardCapture.ResponseCode.class);
	{
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_NO_CARD,
				"No SD card was detected.  Please make sure you have a working, formatted\n" +
				"SD card in the motherboard's SD slot and try again.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_INIT,
				"ReplicatorG was unable to initialize the SD card.  Please make sure that\n" +
				"the SD card works properly.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_PARTITION,
				"ReplicatorG was unable to read the SD card's partition table.  Please check\n" +
				"that the card is partitioned properly.\n" +
				"If you believe your SD card is OK, try resetting your device and restarting\n" +
				"ReplicatorG."
				);
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_FS,
				"ReplicatorG was unable to open the filesystem on the SD card.  Please make sure\n" +
				"that the SD card has a single partition formatted with a FAT16 filesystem.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_ROOT_DIR,
				"ReplicatorG was unable to read the root directory on the SD card.  Please\n"+
				"check to see if the SD card was formatted properly.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_LOCKED,
				"The SD card cannot be written to because it is locked.  Remove the card,\n" +
				"switch the lock off, and try again.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_NO_FILE,
				"ReplicatorG could not find the build file on the SD card.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_GENERIC,"Unknown SD card error.");
	}
	
	/**
	 * Process an SD response code and throw up an appropriate dialog for the user.
	 * @param code the response from the SD request
	 * @return true if the code indicates success; false if the operation should be aborted
	 */
	public boolean processSDResponse(SDCardCapture.ResponseCode code) {
		if (code == SDCardCapture.ResponseCode.SUCCESS) return true;
		String message = sdErrorMap.get(code);
		JOptionPane.showMessageDialog(
//				window,
				null,
				message,
				"SD card error",
				JOptionPane.ERROR_MESSAGE);
		return false;
	}

	@Override
	public int getLinesProcessed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getLinesTotal() {
		// TODO Auto-generated method stub
		return 0;
	}
}
