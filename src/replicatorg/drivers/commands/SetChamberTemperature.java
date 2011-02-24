package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class SetChamberTemperature implements DriverCommand {

	double temperature;
	
	public SetChamberTemperature(double temperature) {
		this.temperature = temperature;
	}
	
	@Override
	public void run(Driver driver) {
		driver.setChamberTemperature(temperature);
	}
}
