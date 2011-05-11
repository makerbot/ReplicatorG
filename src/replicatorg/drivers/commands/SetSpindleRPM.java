package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetSpindleRPM implements DriverCommand {

	double rpm;
	
	public SetSpindleRPM(double rpm) {
		this.rpm = rpm;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setSpindleRPM(rpm);
	}
}