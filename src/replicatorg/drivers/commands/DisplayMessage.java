package replicatorg.drivers.commands;

import java.util.EnumSet;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.InteractiveDisplay;
import replicatorg.drivers.RetryException;

public class DisplayMessage implements DriverCommand {

	String message;
	double seconds;

	/** 
	 * Display a message on the printing device, if supported.
	 * @param seconds The time, in seconds, to display the message for.
	 * 0 indicates that the message should be displayed until superceded.
	 * @param message The text of the message to display.
	 */
	public DisplayMessage(double seconds, String message) {
		this.seconds = seconds;
		this.message = message;
	}
		
	@Override
	public void run(Driver driver) throws RetryException {
		if (driver instanceof InteractiveDisplay) {
			((InteractiveDisplay)driver).displayMessage(seconds,message);
		}
		else 
			Base.logger.severe("driver " + driver + "is not an instance of IntractiveDisplay");
	}	
}