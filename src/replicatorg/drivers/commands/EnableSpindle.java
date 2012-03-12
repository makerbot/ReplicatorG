package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class EnableSpindle implements DriverCommand {
	private final int toolIndex;

	public EnableSpindle(int toolIndex) {
		this.toolIndex = checkNonNegative(toolIndex,
				"Tool indices should not be negative");
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.enableSpindle(toolIndex);
	}
}
