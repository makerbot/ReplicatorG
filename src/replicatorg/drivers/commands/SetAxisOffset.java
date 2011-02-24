package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.machine.model.AxisId;

public class SetAxisOffset implements DriverCommand {

	AxisId axis;
	int offsetId;
	double offset;
	
	public SetAxisOffset(AxisId axis, int offsetId, double offset) {
		this.axis = axis;
		this.offsetId = offsetId;
		this.offset = offset;
	}
	
	@Override
	public void run(Driver driver) {
		switch(axis) {
		case X:
			driver.setOffsetX(offsetId, offset);
			break;
		case Y:
			driver.setOffsetY(offsetId, offset);
			break;
		case Z:
			driver.setOffsetZ(offsetId, offset);
			break;
		}
	}
}