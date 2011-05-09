package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetPlatformTemperature implements DriverCommand {

	double temperature;
	
	public SetPlatformTemperature(double temperature) {
		this.temperature = temperature;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setPlatformTemperature(temperature);
	}	
}