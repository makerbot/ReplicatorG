package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetPlatformTemperature implements DriverCommand {

	private final double temperature;
	private final int toolIndex;

	/**
	 * Set temperature for a specified toolhead
	 * @param temperature
	 * @param toolIndex
	 */
	public SetPlatformTemperature(double temperature, int toolIndex) {
		this.temperature = temperature;
		this.toolIndex = checkNonNegative(toolIndex,
				"Tool indices should not be negative");
	}

	@Override
	public void run(Driver driver) throws RetryException {
		driver.setPlatformTemperature(temperature, toolIndex);
	}	
}