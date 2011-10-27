package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class EnableMotor implements DriverCommand {
	
	long millis = 0;

	public EnableMotor() {
		this.millis = 0;
	}
	
	public EnableMotor(long millis) {
		this.millis = millis;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		if (this.millis != 0) {
			driver.enableMotor(this.millis);
		} else
		{
			driver.enableMotor();
		}
	}
	
	
}