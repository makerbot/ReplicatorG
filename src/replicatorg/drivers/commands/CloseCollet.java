package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class CloseCollet implements DriverCommand {
	
	@Override
	public void run(Driver driver) {
		driver.closeCollet();
	}
}
