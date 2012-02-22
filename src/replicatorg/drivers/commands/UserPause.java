package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.InteractiveDisplay;
import replicatorg.drivers.RetryException;

public class UserPause implements DriverCommand {

	double seconds;
	boolean resetOnTimeout;
	int buttonMask;

	/** 
	 * Display a message on the printing device, if supported.
	 * @param seconds The time, in seconds, to pause for.
	 * 0 indicates that the pause should be indefinite.
	 * @param resetOnTimeout true if the machine should reset if
	 * the pause times out.
	 * @param buttonMask a bitfield representing the buttons to wait
	 * on. 0xff indicates that any button press should continue.
	 */
	public UserPause(double seconds, boolean resetOnTimeout, int buttonMask) {
		this.seconds = seconds;
		this.resetOnTimeout = resetOnTimeout;
		this.buttonMask = buttonMask;
	}
		
	@Override
	public void run(Driver driver) throws RetryException {
		if (driver instanceof InteractiveDisplay) {
			((InteractiveDisplay)driver).userPause(seconds,resetOnTimeout,buttonMask);
		}
	}
}