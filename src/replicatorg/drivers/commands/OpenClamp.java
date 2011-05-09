package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class OpenClamp implements DriverCommand {
	
	int clampIndex;
	
	public OpenClamp(int clampIndex) {
		this.clampIndex = clampIndex;
	}
	
	@Override
	public void run(Driver driver) {
		driver.openClamp(clampIndex);
	}
}
