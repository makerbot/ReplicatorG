package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class EnableFan implements DriverCommand {
	private final int toolhead;

	public EnableFan(int toolhead)
	{
		this.toolhead = checkNonNegative(toolhead,
				"Tool indices should not be negative");
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.enableFan(toolhead);
	}
}
