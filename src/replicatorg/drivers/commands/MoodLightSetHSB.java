package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class MoodLightSetHSB implements DriverCommand {

	int hue;
	int saturation;
	int brightness;
	int fadeSpeed;
	
	public MoodLightSetHSB(int hue, int saturation, int brightness, int fadeSpeed) {
		this.hue = hue; 
		this.saturation = saturation;
		this.brightness = brightness;
		this.fadeSpeed = fadeSpeed;
	}
	@Override
	public void run(Driver driver) throws RetryException {
		driver.moodLightSetHSB(hue, saturation, brightness, fadeSpeed);
	}
}
