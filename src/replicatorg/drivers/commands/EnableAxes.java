package replicatorg.drivers.commands;

import java.util.EnumSet;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;
import replicatorg.machine.model.AxisId;

public class EnableAxes implements DriverCommand {

	EnumSet<AxisId> axes;
	
	public EnableAxes(EnumSet<AxisId> axes) {
		this.axes = axes;
	}
	
	@Override
	public void run(Driver driver) throws RetryException, StopException {
		driver.enableAxes(axes);
	}

}
