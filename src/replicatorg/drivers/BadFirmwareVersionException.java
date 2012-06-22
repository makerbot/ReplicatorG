/**
 * 
 */
package replicatorg.drivers;

import replicatorg.app.Base;

/**
 * Indicate that initialization failed because the machine is using obsolete firmware.
 * This should be used to indicate that the firmware version is absolutely unusable
 * with the current driver.
 * @author phooky
 *
 */
public class BadFirmwareVersionException extends VersionException {
	private static final long serialVersionUID = 6973397918493070849L;
	/** The firmware version that is needed */
	Version needs;
	
	public BadFirmwareVersionException(Version has, Version needs) {
		super(has);
		this.needs = needs;
		Base.logger.warning(getMessage());
	}
	
	public Version getNeeds() { return needs; }

	public String getMessage() {
		return "Firmware version "+getDetected()+" detected; firmware version "+needs+" required.";
	}
	
}
