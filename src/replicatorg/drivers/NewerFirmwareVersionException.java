package replicatorg.drivers;

/**
 * Indicates that a newer firmware version is available for the given hardware.
 * @author phooky
 *
 */
public class NewerFirmwareVersionException extends VersionException {
	private Version available;
	
	public NewerFirmwareVersionException(Version detected, Version available) {
		super(detected);
		this.available = available;
	}
	
	public Version getAvailable() { return available; }
	
	public String getMessage() {
		return "Firmware version "+getDetected()+" is obsolete; firmware version "+
			available+" is now available.";
	}

}
