package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetPlatformTemperature implements DriverCommand {

	double temperature;
	int toolIndex = -1;

	/**
	 * Set temperature, trusting and hoping the right toolhead is active
	 * when we do. 
	 * @param temperature
	 */
	@Deprecated
	public SetPlatformTemperature(double temperature) {
		this.temperature = temperature;
		this.toolIndex  = -1;// do problematic 'getcurrent tool' when comand runs
	}
	
	/**
	 * Set temperature for a specified toolhead
	 * @param temperature
	 * @param toolIndex
	 */
	public SetPlatformTemperature(double temperature, int toolIndex) {
		this.temperature = temperature;
		this.toolIndex = toolIndex;
	}

	@Override
	public void run(Driver driver) throws RetryException {
		if(this.toolIndex == -1)
			driver.setPlatformTemperature(temperature);
		else
			driver.setPlatformTemperature(temperature, toolIndex);
	}	
}