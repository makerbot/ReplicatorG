package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SendBeep implements DriverCommand {

	int frequencyHz;
	int durationMs;
	int effect; 
	
	public SendBeep(int frequencyHz, int durationMs, int effect) {
		this.frequencyHz = frequencyHz;
		this.durationMs = durationMs;
		this.effect  = effect;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.sendBeep(frequencyHz, durationMs, effect);
	}
}
