package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

public class ProgramRewind implements DriverCommand {

	String message;
	
	public ProgramRewind(String message) {
		this.message = message;
	}
	
	@Override
	public void run(Driver driver) throws RetryException, StopException {
		throw new StopException(message, StopException.StopType.PROGRAM_REWIND);
	}
}