/**
 * 
 */
package replicatorg.drivers;

/**
 * Indicate that initialization failed because the machine is using obsolete firmware.
 * This should be used to indicate that the firmware version is absolutely unusable
 * with the current driver.
 * @author phooky
 *
 */
public class BadFirmwareVersionException extends RuntimeException {
	private static final long serialVersionUID = 6973397918493070849L;
	/** The firmware version that was detected */
	Version has;
	/** The firmware version that is needed */
	Version needs;
	
	public BadFirmwareVersionException(Version has, Version needs) {
		super("Firmware v"+has+" detected; firmware v"+needs+" required.");
		this.has = has;
		this.needs = needs;
	}
	
	public Version getHas() { return has; }
	
	public Version getNeeds() { return needs; }
	
}
