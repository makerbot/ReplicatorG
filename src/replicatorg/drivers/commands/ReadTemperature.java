package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

/**
 * Command encapsulation for reading all temperatures
 * @author farmckon
 *
 */
public class ReadTemperature implements DriverCommand {

	///Generic constructor auto-generated
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.readAllTemperatures();
		driver.readAllPlatformTemperatures();
	}
}
