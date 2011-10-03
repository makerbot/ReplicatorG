package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class ToggleAutomatedBuildPlatform implements DriverCommand {

	boolean state;
	public ToggleAutomatedBuildPlatform(boolean state)
	{
		super();
		this.state = state;
	}

	@Override
	public void run(Driver driver) throws RetryException {
		driver.setAutomatedBuildPlatformRunning(state);
	}

}
