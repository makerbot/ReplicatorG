package replicatorg.machine.builder;

import java.util.EnumMap;
import java.util.Map;

import javax.swing.JOptionPane;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.SDCardCapture;
import replicatorg.drivers.SimulationDriver;
import replicatorg.machine.Machine.JobTarget;
import replicatorg.model.GCodeSource;

public class ToRemoteFile implements MachineBuilder {

	// TODO: These are in multiple places.
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
				null,
				message,
				"SD card error",
				JOptionPane.ERROR_MESSAGE);
		return false;
	}
	
	Direct directBuilder;
	
	SDCardCapture sdcc;

	public ToRemoteFile(Driver driver, SimulationDriver simulator, GCodeSource source, String remoteName) {
		// TODO: we might fail here.
		this.sdcc = (SDCardCapture)driver;
		
		if (processSDResponse(sdcc.beginCapture(remoteName))) {
			directBuilder = new Direct(driver, simulator, source);
		}
		else {
			// TODO: Error here.
		}
	}
	
	@Override
	public boolean finished() {
		if (!directBuilder.finished()) { 
			return false;
		}
		
		int totalBytes = sdcc.endCapture();
		Base.logger.info("Captured bytes: " +Integer.toString(totalBytes));
		return true;
	}
	
	@Override
	public void runNext() {
		directBuilder.runNext();
	}

	@Override
	public int getLinesTotal() {
		return directBuilder.getLinesTotal();
	}

	@Override
	public int getLinesProcessed() {
		return directBuilder.getLinesProcessed();
	}

	@Override
	public boolean isInteractive() {
		return false;
	}

	@Override
	public JobTarget getTarget() {
		return JobTarget.REMOTE_FILE;
	}
}
