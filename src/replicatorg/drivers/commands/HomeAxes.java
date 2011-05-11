package replicatorg.drivers.commands;

import java.util.EnumSet;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.machine.model.AxisId;

public class HomeAxes implements DriverCommand {

	EnumSet<AxisId> axes;
	LinearDirection direction;
	double feedrate;
	
	public HomeAxes(EnumSet<AxisId> axes, LinearDirection direction) {
		this.axes = axes;
		this.direction = direction;
		// 0 is a magic flag to tell the driver to use the max feedrate
		this.feedrate = 0;
	}
	
	public HomeAxes(EnumSet<AxisId> axes, LinearDirection direction, double feedrate) {
		this.axes = axes;
		this.direction = direction;
		this.feedrate = feedrate;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		if (direction == LinearDirection.POSITIVE) {
			driver.homeAxes(axes, true, feedrate);
		}
		else {
			driver.homeAxes(axes, false, feedrate);
		}
	}	
}