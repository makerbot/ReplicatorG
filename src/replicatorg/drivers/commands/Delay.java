package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class Delay implements DriverCommand {

	private final long delay;
	
	public Delay(long delay) {
		this.delay = delay;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.delay(delay);
	}
}