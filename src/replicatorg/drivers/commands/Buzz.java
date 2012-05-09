package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class Buzz implements DriverCommand {

	int buzzes;
	int duration;
	int repeats;

	public Buzz(int buzzes, int duration, int repeats) {
		this.buzzes = buzzes; 
		this.duration = duration; 
		this.repeats = repeats; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.buzz(buzzes, duration, repeats);
	}
}
