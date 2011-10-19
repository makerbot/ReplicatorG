package replicatorg.drivers.commands;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

public class StopDataCapture implements DriverCommand {

	@Override
	public void run(Driver driver) throws RetryException, StopException {
		Base.logger.info("Data capture Stopped");
		// TODO: really? is this enough, or should/will we destroy the logger and delete it
		Base.capture = null;
	}

}
