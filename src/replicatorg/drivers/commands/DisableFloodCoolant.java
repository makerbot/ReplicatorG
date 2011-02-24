package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class DisableFloodCoolant implements DriverCommand {
	@Override
	public void run(Driver driver) {
		driver.disableFloodCoolant();
	}
}
