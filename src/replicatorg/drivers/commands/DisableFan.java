package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class DisableFan implements DriverCommand {

	int toolhead = -1; ///lazy auto-detect tool

	public DisableFan() {} 

	
	public DisableFan(int toolhead)
	{
		this.toolhead = toolhead;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.disableFan(toolhead);
	}

}