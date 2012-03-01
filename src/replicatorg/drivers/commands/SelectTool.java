package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SelectTool implements DriverCommand {

	int toolNumber;
	
	public SelectTool(int toolNumber) {
		this.toolNumber = toolNumber; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.getMachine().selectTool(toolNumber);
                driver.selectTool(toolNumber);
	}
}
