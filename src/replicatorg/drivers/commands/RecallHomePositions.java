package replicatorg.drivers.commands;

import java.util.EnumSet;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.machine.model.AxisId;

public class RecallHomePositions implements DriverCommand {

	EnumSet<AxisId> axes;
	
	public RecallHomePositions(EnumSet<AxisId> axes) {
		this.axes = axes;
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.recallHomePositions(axes);
	}
}
