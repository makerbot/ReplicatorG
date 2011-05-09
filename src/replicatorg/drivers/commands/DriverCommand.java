package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

public interface DriverCommand {
	public enum AxialDirection {
		CLOCKWISE,
		COUNTERCLOCKWISE,
	}
	
	public enum LinearDirection {
		POSITIVE,
		NEGATIVE,
	}
	
	public void run(Driver driver) throws RetryException, StopException;
}
