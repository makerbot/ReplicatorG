package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.util.Point5d;

public class SetCurrentPosition implements DriverCommand {

	Point5d point;
	
	public SetCurrentPosition(Point5d point) {
		this.point = point;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setCurrentPosition(point);
	}
}