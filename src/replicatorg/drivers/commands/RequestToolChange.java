package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class RequestToolChange implements DriverCommand {

	int toolIndex;
	int timeout;
	
	public RequestToolChange(int toolIndex, int timeout) {
		this.toolIndex = toolIndex; 
		this.timeout = timeout;
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.requestToolChange(toolIndex, timeout);
	}
}
