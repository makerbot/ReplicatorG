package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class EnableFan implements DriverCommand {

	int toolhead = -1; /// lazy lookup current tool

	public EnableFan() {} 

	
	public EnableFan(int toolhead)
	{
		this.toolhead = toolhead;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.enableFan(toolhead);
	}
}
