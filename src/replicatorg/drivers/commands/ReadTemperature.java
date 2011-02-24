package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class ReadTemperature implements DriverCommand {

	@Override
	public void run(Driver driver) throws RetryException {
		driver.readTemperature();
	}
}
