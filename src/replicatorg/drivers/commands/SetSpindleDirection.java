package replicatorg.drivers.commands;

import static replicatorg.util.Preconditions.*;

import replicatorg.drivers.Driver;
import replicatorg.machine.model.ToolModel;

public class SetSpindleDirection implements DriverCommand {
	private final int toolIndex;
	private final AxialDirection direction;
	
	public SetSpindleDirection(AxialDirection direction, int toolIndex) {
		this.toolIndex = checkNonNegative(toolIndex,
				"Tool indices should not be negative");
		this.direction = direction;
	}
	
	@Override
	public void run(Driver driver) {
		if (direction == AxialDirection.CLOCKWISE) {
			driver.setSpindleDirection(ToolModel.MOTOR_CLOCKWISE, toolIndex);
		}
		else {
			driver.setSpindleDirection(ToolModel.MOTOR_COUNTER_CLOCKWISE, toolIndex);
		}
	}
}
