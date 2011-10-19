package replicatorg.drivers.commands;

import replicatorg.app.Base;
import replicatorg.app.DataCapture;
import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

/**
 * DriverCommand class to create a new DataCapture object
 */
public class StartDataCapture implements DriverCommand {

	String filename;/// target filename to log data into
	
	public StartDataCapture(String filename) {
		this.filename = filename;
	}

	/// constructs the target logger, which should self.register and start logging	
	public void run(Driver driver) throws RetryException, StopException {
		// TODO Auto-generated method stub
		Base.logger.info("Data capture started, filename: " + filename);
		Base.capture = new DataCapture(filename);
	}

}
