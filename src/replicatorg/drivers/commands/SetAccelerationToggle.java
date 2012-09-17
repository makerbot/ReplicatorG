package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetAccelerationToggle implements DriverCommand {

	boolean on;

	public SetAccelerationToggle(boolean on) {
		this.on = on;
	}

	@Override
	public void run(Driver driver) throws RetryException {
		driver.setAccelerationToggle(on);
	}
}
