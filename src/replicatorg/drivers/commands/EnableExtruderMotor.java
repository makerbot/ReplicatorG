package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class EnableExtruderMotor implements DriverCommand {
	
	long millis = 0;

	public EnableExtruderMotor() {
		this.millis = 0;
	}
	
	public EnableExtruderMotor(long millis) {
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