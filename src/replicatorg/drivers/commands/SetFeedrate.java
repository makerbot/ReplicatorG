package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetFeedrate implements DriverCommand {

	double feedrate;
	
	public SetFeedrate(double feedrate) {
		this.feedrate = feedrate;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setFeedrate(feedrate);
	}
}