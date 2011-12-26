package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class EnableExtruderMotor implements DriverCommand {
	
	long millis = 0;
	int toolhead = -1;
	
	public EnableExtruderMotor() {}
	
	public EnableExtruderMotor(int toolhead) {
		this.millis = 0;
		this.toolhead = toolhead;
	}
	
	public EnableExtruderMotor(long millis) {
		this.millis = millis;
	}
	
	public EnableExtruderMotor(long millis, int toolhead) {
		this.millis = millis;
		this.toolhead = toolhead;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		if (this.millis != 0) {
			driver.enableMotor(this.millis,toolhead);
		} else
		{
			driver.enableMotor(toolhead);
		}
	}
	
	
}