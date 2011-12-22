package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.machine.model.ToolModel;

public class SetMotorDirection implements DriverCommand {

	AxialDirection direction;
	int toolhead = -1; /// lazy autodetect
	
	public SetMotorDirection(AxialDirection direction) {
		this.direction = direction;
	}

	public SetMotorDirection(AxialDirection direction, int toolhead) {
		this.direction = direction;
		this.toolhead = toolhead;
	}

	@Override
	public void run(Driver driver) {
		// TODO Auto-generated method stub
		if (direction == AxialDirection.CLOCKWISE) {
			driver.setMotorDirection(ToolModel.MOTOR_CLOCKWISE, toolhead);
		}
		else {
			driver.setMotorDirection(ToolModel.MOTOR_COUNTER_CLOCKWISE, toolhead);
		}
	}
}
