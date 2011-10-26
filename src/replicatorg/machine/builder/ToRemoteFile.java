package replicatorg.machine.builder;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.SDCardCapture;
import replicatorg.drivers.SimulationDriver;
import replicatorg.machine.Machine.JobTarget;
import replicatorg.model.GCodeSource;

/** 
 * 
 * 
 *
 */
public class ToRemoteFile implements MachineBuilder {
	
	Direct directBuilder;
	
	SDCardCapture sdcc;
	public boolean setupFailed = true;

	public ToRemoteFile(Driver driver, SimulationDriver simulator, GCodeSource source, String remoteName) {
		if(!(driver instanceof SDCardCapture))
		{
			Base.logger.log(Level.WARNING, 
					"Build to remote file requires a driver with SDCardCapture!");
			return;
		}
		
		sdcc = (SDCardCapture)driver;
		
		if (SDCardCapture.ResponseCode.processSDResponse(sdcc.beginCapture(remoteName))) {
			directBuilder = new Direct(driver, simulator, source);
			setupFailed = false;
		}
	}
	
	@Override
	public boolean finished() {
		// if we got an error response, we don't have any work to do, so just return
		if(setupFailed)
			return true;
		if (!directBuilder.finished())
			return false;
		
		int totalBytes = sdcc.endCapture();
		Base.logger.info("Captured bytes: " +Integer.toString(totalBytes));
		return true;
	}
	
	@Override
	public void runNext() {
		if(directBuilder != null)
			directBuilder.runNext();
	}

	@Override
	public int getLinesTotal() {
		if(directBuilder == null)
			return -1;
		return directBuilder.getLinesTotal();
	}

	@Override
	public int getLinesProcessed() {
		if(directBuilder == null)
			return -1;
		return directBuilder.getLinesProcessed();
	}

	@Override
	public boolean isInteractive() {
		return false;
	}

	@Override
	public JobTarget getTarget() {
		return JobTarget.REMOTE_FILE;
	}
}
