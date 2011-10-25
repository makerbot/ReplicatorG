package replicatorg.drivers.commands;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

/**
 * This is a class for making notes on DataCaptures directly to the base logger
 * at data level 'info
 */
public class DataCaptureNote implements DriverCommand {

	String message;

	/// Constructs a DataCaptureNote with a messsage	
	public DataCaptureNote( String message ) {
		this.message = message;
	}

	/// Writes the constructed note to teh Base.logger as info	
	@Override
	public void run(Driver driver) throws RetryException, StopException {
		Base.logger.info("writing message to data capture: " + message);
		if(Base.capture != null)
			Base.capture.WriteMessage(message);
		else 
			Base.logger.severe("trying to write a log message to nonexistant log file");
	}
}
