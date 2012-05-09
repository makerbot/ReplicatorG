package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetAxisStepsPerMM implements DriverCommand {

	double x, y, z, a;

	public SetAxisStepsPerMM(double x, double y, double z, double a) {
		this.x = x; 
		this.y = y; 
		this.z = z; 
		this.a = a; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setAxisStepsPerMM(x,y,z,a);
	}
}
