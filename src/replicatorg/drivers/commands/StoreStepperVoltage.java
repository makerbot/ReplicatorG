package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

/**
 * Command to store the stepper voltage to EEPROM, but not set the current
 * stepper voltage.
 * @author farmckon
 *
 */
public class StoreStepperVoltage implements DriverCommand {

	int stepperId;
	int voltageScale;
	
	public StoreStepperVoltage(int stepperId, int voltageScale) {
		this.stepperId = stepperId;
		this.voltageScale = voltageScale;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.storeStepperVoltage(stepperId, voltageScale);
	}
}
