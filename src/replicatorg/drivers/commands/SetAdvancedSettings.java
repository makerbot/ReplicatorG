package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetAdvancedSettings implements DriverCommand {

	double s, t, z;

	public SetAdvancedSettings(double s, double t, double z) {
		this.s = s; 
		this.t = t; 
		this.z = z; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setAdvancedSettings(s,t,z);
	}
}
