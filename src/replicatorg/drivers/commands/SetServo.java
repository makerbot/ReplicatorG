package replicatorg.drivers.commands;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.PenPlotter;
import replicatorg.drivers.RetryException;

public class SetServo implements DriverCommand {
	
	int servoIndex;
	double position;
	
	public SetServo(int servoIndex, double position) {
		this.servoIndex = servoIndex;
		this.position = position;
	}
	
	@Override
	public void run(Driver driver) throws RetryException {
		if (driver instanceof PenPlotter) {
			((PenPlotter)driver).setServoPos(servoIndex, position);
		}
		else {
			Base.logger.severe("Driver doeesn't support pen plotter extension");
		}

	}
}
