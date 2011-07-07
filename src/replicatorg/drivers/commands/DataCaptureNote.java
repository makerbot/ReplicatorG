package replicatorg.drivers.commands;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

public class DataCaptureNote implements DriverCommand {

	String message;
	
	public DataCaptureNote( String message ) {
		this.message = message;
	}
	
	@Override
	public void run(Driver driver) throws RetryException, StopException {
		Base.logger.info("writing message to data capture: " + message);
		Base.capture.WriteMessage(message);
	}
}
