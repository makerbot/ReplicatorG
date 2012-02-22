package replicatorg.drivers.commands;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.InteractiveDisplay;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.StopException;

public class PlaySong implements DriverCommand {

	int songId;
	public PlaySong(double codeValue) {
		this.songId = (int)codeValue;
	}

	@Override
	public void run(Driver driver) throws RetryException, StopException {
		///send a song request to the printer so it plays a note or song from it's 
		///song/tone list
		if (driver instanceof InteractiveDisplay) {
			((InteractiveDisplay)driver).playSong(this.songId);
		}
		else 
			Base.logger.severe("driver " + driver + "is not an instance of IntractiveDisplay");


	}

}
