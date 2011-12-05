package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SetStepperVoltage implements DriverCommand {

	int stepperId;
	int voltageScale;
	
	public SetStepperVoltage(int stepperId, int voltageScale) {
		this.stepperId = stepperId;
		this.voltageScale = voltageScale;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setStepperVoltage(stepperId, voltageScale);
	}
}
