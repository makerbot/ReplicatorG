package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetMotorSpeedPWM implements DriverCommand {

	private final int pwm;
	private final int toolhead;

	public SetMotorSpeedPWM(int pwm, int toolhead) {
		this.pwm = pwm;
		this.toolhead = checkNonNegative(toolhead,
				"Tool indices should not be negative");
	}

	@Override
	public void run(Driver driver) throws RetryException {
		driver.setMotorSpeedPWM(pwm,toolhead);
	}
}