package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetTemperature implements DriverCommand {

	private final double temperature;
	private final int toolhead;
	
	/**
	 * Set temperature, specifying the toolhead index. 
	 * @param temperature
	 * @param toolIndex
	 */
	public SetTemperature(double temperature, int toolIndex) {
		this.temperature = temperature;
		this.toolhead = checkNonNegative(toolIndex,
				"Tool indices should not be negative");
	}

	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setTemperature(temperature, toolhead);
	}
}
