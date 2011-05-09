package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetMotorSpeedRPM implements DriverCommand {

	double rpm;
	
	public SetMotorSpeedRPM(double rpm) {
		this.rpm = rpm;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setMotorRPM(rpm);
	}
}