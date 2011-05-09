package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class InvalidatePosition implements DriverCommand {

	@Override
	public void run(Driver driver) {
		driver.invalidatePosition();
	}
}
