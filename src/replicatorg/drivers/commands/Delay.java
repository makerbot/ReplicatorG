package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class Delay implements DriverCommand {

	long delay;
	int toolhead = -1; ///lazy lookup toolhead
	public Delay(long delay) {
		this.delay = delay;
	}
	
	public Delay(long delay, int toolhead) {
		this.delay = delay;
		this.toolhead = toolhead;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.delay(delay); ///toolhead is ignored for current gen machines
	}
}