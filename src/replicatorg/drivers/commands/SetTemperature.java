package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetTemperature implements DriverCommand {

	double temperature;
	
	/**
	 * Set temperature, trusting and hoping the right toolhead is active
	 * when we do. 
	 * @param temperature
	 */
	@Deprecated
	public SetTemperature(double temperature) {
		this.temperature = temperature;
	}

	/**
	 * Set temperature, specifying the toolhead index. 
	 * @param temperature
	 * @param toolIndex
	 */
	public SetTemperature(double temperature, int toolIndex) {
		this.temperature = temperature;
	}

	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setTemperature(temperature);
	}
}
