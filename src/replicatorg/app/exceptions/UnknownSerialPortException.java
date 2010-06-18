/**
 * 
 */
package replicatorg.app.exceptions;

/**
 * Indicate that the serial port requested does not currently exist on the device.
 * Possibly indicates that a serial port has been removed, or that the request
 * comes from a stored preference that does not apply to the current configuration
 * or platform.
 * @author phooky
 *
 */
public class UnknownSerialPortException extends SerialException {
	private String name;
	
	/**
	 * Create an exception indicating that the requested serial port name cannot be found.
	 * @param name the name of the serial port that caused the exception
	 */
	public UnknownSerialPortException(String name) {
		super("The serial port named '"+name+"' could not be found.");
		this.name = name;
	}
	/**
	 * Find the name that could not be resolved.
	 * @return the ostensible serial port name
	 */
	public String getName() {
		return name;
	}
}
