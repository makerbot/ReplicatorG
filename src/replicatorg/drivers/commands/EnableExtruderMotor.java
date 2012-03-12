package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class EnableExtruderMotor implements DriverCommand {
	
	private final long millis;
	private final int toolhead;
	
	public EnableExtruderMotor(int toolhead) {
		this(0, toolhead);
	}
	
	public EnableExtruderMotor(long millis, int toolhead) {
		this.millis = millis;
		this.toolhead = checkNonNegative(toolhead,
				"Tool indices should not be negative");
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