package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class ChangeGearRatio implements DriverCommand {
	
	int gearRatio;
	
	public ChangeGearRatio(int gearRatio) {
		this.gearRatio = gearRatio;
	}
	
	@Override
	public void run(Driver driver) {
		driver.changeGearRatio(gearRatio);
	}
}
