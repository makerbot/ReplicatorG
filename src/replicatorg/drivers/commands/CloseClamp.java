package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class CloseClamp implements DriverCommand {
	
	int clampIndex;
	
	public CloseClamp(int clampIndex) {
		this.clampIndex = clampIndex;
	}
	
	@Override
	public void run(Driver driver) {
		driver.closeClamp(clampIndex);
	}
}
