package replicatorg.machine.builder;

import java.io.FileNotFoundException;
import java.io.IOException;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.SDCardCapture;
import replicatorg.drivers.SimulationDriver;
import replicatorg.machine.Machine.JobTarget;
import replicatorg.model.GCodeSource;

/**
 * Build to a file on the remote machine.
 * This just wraps MachineBuilder.Direct() with some extra code to signal the machine
 * that it should be saving to a file.
 * @author mattmets
 *
 */
public class ToLocalFile implements MachineBuilder {
	
	Direct directBuilder;
	
	SDCardCapture sdcc;

	public ToLocalFile(Driver driver, SimulationDriver simulator, GCodeSource source, String remoteName) {
		// TODO: we might fail here.
		this.sdcc = (SDCardCapture)driver;
		
		try {
			sdcc.beginFileCapture(remoteName);
		} catch (FileNotFoundException e) {
			Base.logger.fine("error!");
			// TODO: Report an error and finish.
		}
		
		directBuilder = new Direct(driver, simulator, source);
	}
	
	@Override
	public boolean finished() {
		if (!directBuilder.finished()) { 
			return false;
		}
		
		try {
			sdcc.endFileCapture();
		} catch (IOException e) {
			// TODO: Report an error here?
		}
		
		Base.logger.info("Finished writing to file!");
		return true;
	}
	
	@Override
	public void runNext() {
		directBuilder.runNext();
	}

	@Override
	public int getLinesTotal() {
		return directBuilder.getLinesTotal();
	}

	@Override
	public int getLinesProcessed() {
		return directBuilder.getLinesProcessed();
	}

	@Override
	public boolean isInteractive() {
		return false;
	}

	@Override
	public JobTarget getTarget() {
		return JobTarget.FILE;
	}
}
