package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class EnableFloodCoolant implements DriverCommand {
	
	@Override
	public void run(Driver driver) {
		driver.enableFloodCoolant();
	}
}
