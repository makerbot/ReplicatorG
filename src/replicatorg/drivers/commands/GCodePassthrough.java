package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;

public class GCodePassthrough implements DriverCommand {

	String command;
	
	public GCodePassthrough(String command) {
		this.command = command;
	}
	
	@Override
	public void run(Driver driver) {
		driver.executeGCodeLine(command);
	}
}
