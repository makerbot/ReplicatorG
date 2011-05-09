package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class EnableSpindle implements DriverCommand {

	public EnableSpindle() {};
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.enableSpindle();
	}
}
