package replicatorg.drivers.commands;


import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

public class ProgramEnd implements DriverCommand {

	String message;
	
	public ProgramEnd(String message) {
		this.message = message;
	}
	
	@Override
	public void run(Driver driver) throws RetryException, StopException {
		// TODO: This should be handled differently, so that the machine
		// controller can display it correctly (as a message box, toolbar notification, etc).
		throw new StopException(message, StopException.StopType.UNCONDITIONAL_HALT);
	}
}

