package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetTemperature implements DriverCommand {

	double temperature;
	int toolhead = -1;
	
	/**
	 * Set temperature, trusting and hoping the right toolhead is active
	 * when we do. 
	 * @param temperature
	 */
	@Deprecated
	public SetTemperature(double temperature) {
		this.temperature = temperature;
		this.toolhead = -1;/// do problematic 'get current toolhead
	}

	/**
	 * Set temperature, specifying the toolhead index. 
	 * @param temperature
	 * @param toolIndex
	 */
	public SetTemperature(double temperature, int toolIndex) {
		this.temperature = temperature;
		this.toolhead = toolIndex;
	}

	
	@Override
	public void run(Driver driver) throws RetryException {
		if (this.toolhead == -1)
			driver.setTemperature(temperature); /// do problematic 'get current toolhead
		else
			driver.setTemperature(temperature, toolhead);
	}
}
