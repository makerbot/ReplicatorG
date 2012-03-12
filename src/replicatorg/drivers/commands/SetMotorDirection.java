package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.machine.model.ToolModel;

public class SetMotorDirection implements DriverCommand {

	private final AxialDirection direction;
	private final int toolhead;
	
	public SetMotorDirection(AxialDirection direction, int toolhead) {
		this.direction = direction;
		this.toolhead = checkNonNegative(toolhead,
				"Tool indices should not be negative");
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
