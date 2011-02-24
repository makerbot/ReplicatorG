package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class WaitUntilBufferEmpty implements DriverCommand {

	@Override
	public void run(Driver driver) throws RetryException {
		if (!driver.isBufferEmpty()) {
			throw new RetryException();
		}
	}
}
