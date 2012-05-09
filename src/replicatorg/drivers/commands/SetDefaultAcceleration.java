package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetDefaultAcceleration implements DriverCommand {

	double s, t;

	public SetDefaultAcceleration(double s, double t) {
		this.s = s; 
		this.t = t; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setDefaultAcceleration(s, t);
	}
}
