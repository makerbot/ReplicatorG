package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetMaxSpeedChange implements DriverCommand {

	double x, y, z, a;

	public SetMaxSpeedChange(double x, double y, double z, double a) {
		this.x = x; 
		this.y = y; 
		this.z = z; 
		this.a = a; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setMaxSpeedChange(x,y,z,a);
	}
}
