package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class MoodLightSetRGB implements DriverCommand {

	int red;
	int green;
	int blue;
	int fadeSpeed;
	int writeToEeprom;
	
	public MoodLightSetRGB(int red, int green, int blue, int fadeSpeed, int writeToEeprom) {
		this.red = red; 
		this.green = green;
		this.blue = blue;
		this.fadeSpeed = fadeSpeed;
		this.writeToEeprom = writeToEeprom;
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.moodLightSetRGB(red, green, blue, fadeSpeed, writeToEeprom);
	}
}
