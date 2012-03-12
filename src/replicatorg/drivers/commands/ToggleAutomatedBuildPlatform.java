package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class ToggleAutomatedBuildPlatform implements DriverCommand {
	private final boolean state;
	private final int toolhead;
	
	public ToggleAutomatedBuildPlatform(boolean state, int toolhead)
	{
		this.state = state;
		this.toolhead = checkNonNegative(toolhead,
				"Tool indices should not be negative");
	}

	@Override
	public void run(Driver driver) throws RetryException {
		driver.setAutomatedBuildPlatformRunning(state, toolhead);
	}

}
