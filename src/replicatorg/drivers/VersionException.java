/**
 * 
 */
package replicatorg.drivers;

/**
 * @author phooky
 *
 */
public class VersionException extends RuntimeException {
	private Version detected;
	public VersionException(Version detected) {
		this.detected = detected;
	}
	
	public Version getDetected() { return detected; }
	
	public String getMessage() { 
		return "Generic Version Exception on version " + getDetected().toString();
	}
}
