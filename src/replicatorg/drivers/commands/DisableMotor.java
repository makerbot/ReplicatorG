package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class DisableMotor implements DriverCommand {
	int toolhead = -1 ; ///
	

	public DisableMotor() {}

	public DisableMotor(int toolhead) {
		this.toolhead = toolhead;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.disableMotor(toolhead);
	}
}
