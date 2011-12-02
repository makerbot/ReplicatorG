package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;

public class SendBeep implements DriverCommand {

	int frequencyHz;
	int durationMs;
	
	public SendBeep(int frequencyHz, int durationMs) {
		this.frequencyHz = frequencyHz;
		this.durationMs = durationMs;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.sendBeep(frequencyHz, durationMs);
	}
}
