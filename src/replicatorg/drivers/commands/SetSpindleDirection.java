package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.machine.model.ToolModel;

public class SetSpindleDirection implements DriverCommand {


	AxialDirection direction;
	
	public SetSpindleDirection(AxialDirection direction) {
		this.direction = direction;
	}
	
	@Override
	public void run(Driver driver) {
		// TODO Auto-generated method stub
		if (direction == AxialDirection.CLOCKWISE) {
			driver.setSpindleDirection(ToolModel.MOTOR_CLOCKWISE);
		}
		else {
			driver.setSpindleDirection(ToolModel.MOTOR_COUNTER_CLOCKWISE);
		}
	}
}
