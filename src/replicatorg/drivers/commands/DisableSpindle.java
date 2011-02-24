package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class DisableSpindle implements DriverCommand {

	public DisableSpindle() {};
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.disableSpindle();
	}
}
