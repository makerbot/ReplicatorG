package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetMotorSpeedPWM implements DriverCommand {

	int pwm;
	int toolhead = -1; /// by default, get current tool at dispatch time

	public SetMotorSpeedPWM(int pwm) {
		this.pwm = pwm;
	}

	public SetMotorSpeedPWM(int pwm, int toolhead) {
		this.pwm = pwm;
		this.toolhead = toolhead;
	}

	@Override
	public void run(Driver driver) throws RetryException {
		driver.setMotorSpeedPWM(pwm,toolhead);
	}
}