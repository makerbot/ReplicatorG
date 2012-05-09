package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class MoodLightPlayScript implements DriverCommand {

	int scriptId;
	int writeToEeprom;
	
	public MoodLightPlayScript(int scriptId, int writeToEeprom) {
		this.scriptId	   = scriptId; 
		this.writeToEeprom = writeToEeprom; 
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.moodLightPlayScript(scriptId, writeToEeprom);
	}
}
