package replicatorg.machine;

import replicatorg.drivers.Driver;
import replicatorg.drivers.DriverQueryInterface;
import replicatorg.drivers.SimulationDriver;
import replicatorg.drivers.commands.DriverCommand;
import replicatorg.machine.MachineController.JobTarget;
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

public interface MachineControllerInterface {
	
	public Driver getDriver();
	public SimulationDriver getSimulatorDriver();

	// Register to receive notifications when the machine changes state
	public void addMachineStateListener(MachineListener listener);
	public void removeMachineStateListener(MachineListener listener);
	
	// Reset the state of the machine controller, or dispose of it.
	public void reset();
	public void dispose();
	
	// Connect or disconnect from the physical machine
	public void connect();
	public void disconnect();

	// Get information about the physical machine
	public MachineModel getModel();
	public String getMachineName();
	
	// Control the state machine
	public void estimate();
	public boolean simulate();
	public boolean execute();
	public boolean buildRemote(String remoteName);
	public void buildToFile(String path);
	public void upload(String remoteName);
	public void setCodeSource(GCodeSource source);
	
	public void pause();
	public void unpause();
	
	// Halt the machine immediately
	public void stopMotion();
		
	// Run a command on the driver 
	public void runCommand(DriverCommand command);
	
	// Query the machine controller
	public MachineState getMachineState();
	
	public int getLinesProcessed();
	public JobTarget getTarget();
	public boolean isPaused();
	public boolean isInitialized();
	public boolean isSimulating();
	public boolean isInteractiveTarget();
	
	// Query the driver
	public DriverQueryInterface getDriverQueryInterface();
}
