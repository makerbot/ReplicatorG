package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetExtruderStepsPerMM implements DriverCommand {

	double a;

	public SetExtruderStepsPerMM(double a) {
		this.a = a; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setExtruderStepsPerMM(a);
	}
}
