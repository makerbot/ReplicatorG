package replicatorg.machine.builder;

import java.util.logging.Level;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.SDCardCapture;
import replicatorg.machine.Machine.JobTarget;

/** Helper class to build on a machine from a remote file.
 * 
 * @author mattmets
 *
 */
public class UsingRemoteFile implements MachineBuilder{

	SDCardCapture sdcc;
	boolean setupFailed = true;
	
	public UsingRemoteFile(Driver driver, String remoteName) {
		if(!(driver instanceof SDCardCapture))
		{
			Base.logger.log(Level.WARNING, 
					"Build using remote file requires a driver with SDCardCapture!");
			return;
		}
		
		sdcc = (SDCardCapture)driver;;
		
		if (SDCardCapture.ResponseCode.processSDResponse(sdcc.playback(remoteName))) {
			setupFailed = false;
		}
	}
	
	@Override
	public boolean finished() {
		return (sdcc.isFinished() || setupFailed);
	}

	@Override
	public void runNext() {
	}
	
	@Override
	public int getLinesProcessed() {
		return 0;
	}

	@Override
	public int getLinesTotal() {
		return 0;
	}

	@Override
	public boolean isInteractive() {
		return true;
	}
	
	@Override
	public JobTarget getTarget() {
		return JobTarget.MACHINE;
	}
}
