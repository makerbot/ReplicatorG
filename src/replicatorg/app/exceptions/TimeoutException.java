package replicatorg.app.exceptions;

import replicatorg.app.util.serial.Serial;

/**
 * Timeout exceptions are thrown when a serial operation has waited longer than the serial timeout
 * period.
 * @author phooky
 *
 */
public class TimeoutException extends Exception {
	private static final long serialVersionUID = 110136234567896299L;

	public Serial serial;
	
	public TimeoutException(Serial serial)
	{
		this.serial = serial;
	}
}
