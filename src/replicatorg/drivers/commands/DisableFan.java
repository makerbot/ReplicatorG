package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class DisableFan implements DriverCommand {

	private final int toolhead;

	public DisableFan(int toolhead)
	{
		this.toolhead = checkNonNegative(toolhead,
				"Tool indices should not be negative");
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.disableFan(toolhead);
	}

}