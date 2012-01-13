package replicatorg.drivers.commands;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.InteractiveDisplay;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;
import java.util.regex.*;

/// Command dispatcher to update the build complete percentage
public class SetBuildPercent implements DriverCommand {

	int percentDone; 
	String comment;
	String printName= "RepG Build";
	
	public SetBuildPercent(double codeValue, String comment) {
		this.percentDone = (int)codeValue;
		this.comment = comment;
		Matcher m = Pattern.compile("'.*'").matcher(comment);
		if ( m.find() )
			this.printName = m.group();

	}

	@Override
	public void run(Driver driver) throws RetryException, StopException {

		if (driver instanceof InteractiveDisplay) {
			/// TRICKY: for ui, must happen before first update of build #
			if(percentDone <= 0)
				((InteractiveDisplay)driver).sendBuildStartNotification(this.printName,0);
			((InteractiveDisplay)driver).updateBuildPercent(this.percentDone);			
			/// TRICKY: for ui, must happen after last update of build #
			if(percentDone >= 100)
				((InteractiveDisplay)driver).sendBuildEndNotification(0);
		}
		else 
			Base.logger.severe("driver " + driver + "is not an instance of IntractiveDisplay");
	}	
}
