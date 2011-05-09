package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetMotorSpeedPWM implements DriverCommand {

	int pwm;
	
	public SetMotorSpeedPWM(int pwm) {
		this.pwm = pwm;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setMotorSpeedPWM(pwm);
	}
}