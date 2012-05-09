package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetBuzzerRepetitions implements DriverCommand {

	int repeats;

	public SetBuzzerRepetitions(int repeats) {
		this.repeats = repeats; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setBuzzerRepetitions(repeats);
	}
}
