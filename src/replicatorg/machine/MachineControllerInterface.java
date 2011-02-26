package replicatorg.machine;

import replicatorg.app.ui.MainWindow;
import replicatorg.drivers.Driver;
import replicatorg.drivers.SimulationDriver;
import replicatorg.machine.model.MachineModel;
import replicatorg.model.GCodeSource;


/**
 *  Anything that wants to talk to the machine controller should do so through here. The goal here
 *  is to make this into a multiprocess-safe message interface, so that everything below this (which is
 * currently the machine driver, simulator, and estimator) explicitly don't have to deal with it.
 * 
 * The MachineController that implements this is probably a single thread, with worker threads to handle
 * serial communications or other things.
 * 
 * This interface should allow for the configuration and control of a single 3d printer, and another interface
 * is required to provide a directory of available printers, job queueing, etc.
 * 
 * @author matt.mets
 *
 */
// 
// 

public interface MachineControllerInterface {

	/**
	 * Get the machine state.  This is a snapshot of the state when the method was called.
	 */
	public MachineState getMachineState();
	
	public Driver getDriver();

	public SimulationDriver getSimulatorDriver();
	
	public void buildToFile(String path);
	
	public int getLinesProcessed();
	
	public void reset();
	
	public boolean isInitialized();
	
	public void dispose();
	
	public void setCodeSource(GCodeSource source);
	
	public void setMainWindow(MainWindow window);
	
	public void addMachineStateListener(MachineListener listener);
	
	public void removeMachineStateListener(MachineListener listener);
	
	public boolean buildRemote(String remoteName);
	
	public MachineModel getModel();
	
	public void connect();
	
	public void disconnect();
	
	public void estimate();
	
	public void stop();
	
	public boolean execute();
	
	public boolean simulate();
	
	public String getName();
	
	public boolean isPaused();
	
	public void pause();
	
	public void unpause();
	
	public void upload(String remoteName);
}
