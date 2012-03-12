package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetMotorSpeedRPM implements DriverCommand {

	private final double rpm;
	private final int toolhead;
	
	public SetMotorSpeedRPM(double rpm, int toolhead) {
		this.rpm = rpm;
		this.toolhead = checkNonNegative(toolhead,
				"Tool indices should not be negative");
	}

	@Override
	public void run(Driver driver) throws RetryException {
		driver.setMotorRPM(rpm,toolhead);

	}
}