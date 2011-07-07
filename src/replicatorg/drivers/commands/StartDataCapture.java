package replicatorg.drivers.commands;

import replicatorg.app.Base;
import replicatorg.app.DataCapture;
import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

public class StartDataCapture implements DriverCommand {

	String filename;
	
	public StartDataCapture(String filename) {
		this.filename = filename;
	}
	
	public void run(Driver driver) throws RetryException, StopException {
		// TODO Auto-generated method stub
		Base.logger.info("Data capture started, filename: " + filename);
		Base.capture = new DataCapture(filename);
	}

}
