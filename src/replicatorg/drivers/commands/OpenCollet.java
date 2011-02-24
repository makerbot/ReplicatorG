package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class OpenCollet implements DriverCommand {
	
	@Override
	public void run(Driver driver) {
		driver.openCollet();
	}
}
