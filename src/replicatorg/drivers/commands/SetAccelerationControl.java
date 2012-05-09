package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetAccelerationControl implements DriverCommand {

	double s;

	public SetAccelerationControl(double s) {
		this.s = s; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setAccelerationControl(s);
	}
}
