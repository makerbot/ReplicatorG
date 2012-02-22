package replicatorg.drivers.commands;

import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import java.awt.Color;


public class SetLedStrip implements DriverCommand {

	Color color;
	int effectId;
	
	public SetLedStrip(	Color color, int effectId) {
		this.color= color;
		this.effectId = effectId;
	}

	
	@Override
	public void run(Driver driver) throws RetryException {
		driver.setLedStrip(color, effectId );
	}
}
