package replicatorg.machine.builder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import javax.swing.JOptionPane;

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
	public boolean setupFailed = true;

	public ToLocalFile(Driver driver, SimulationDriver simulator, GCodeSource source, String remoteName) {
		if(!(driver instanceof SDCardCapture))
		{
			Base.logger.log(Level.WARNING, 
					"Build to a file requires a driver with SDCardCapture!");
			return;
		}
		
		sdcc = (SDCardCapture)driver;
		
		try {
			sdcc.beginFileCapture(remoteName);
			directBuilder = new Direct(driver, simulator, source);
			setupFailed = false;
		} catch (FileNotFoundException e) {
			Base.logger.log(Level.WARNING, "Build to file failed: File Not Found!");
		}
	}
	
	@Override
	public boolean finished() {
		if(setupFailed)
			return true;
		if(!directBuilder.finished()) 
			return false;
		
		try {
			sdcc.endFileCapture();
			Base.logger.info("Finished writing to file!");
		} catch (IOException e) {
			Base.logger.log(Level.WARNING, "Could not finish writing to file");
		}
		
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
		return JobTarget.FILE;
	}
}
