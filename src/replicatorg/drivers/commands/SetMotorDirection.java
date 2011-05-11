package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.machine.model.ToolModel;

public class SetMotorDirection implements DriverCommand {

	AxialDirection direction;
	
	public SetMotorDirection(AxialDirection direction) {
		this.direction = direction;
	}
	
	@Override
	public void run(Driver driver) {
		// TODO Auto-generated method stub
		if (direction == AxialDirection.CLOCKWISE) {
			driver.setMotorDirection(ToolModel.MOTOR_CLOCKWISE);
		}
		else {
			driver.setMotorDirection(ToolModel.MOTOR_COUNTER_CLOCKWISE);
		}
	}
}
