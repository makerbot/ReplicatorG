/*
 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package replicatorg.machine;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.GCode;
import replicatorg.app.GCodeEnumeration;
import replicatorg.app.GCodeParser;
import replicatorg.drivers.Driver;
import replicatorg.drivers.DriverQueryInterface;
import replicatorg.drivers.EstimationDriver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SimulationDriver;
import replicatorg.drivers.StopException;
import replicatorg.drivers.commands.DriverCommand;
import replicatorg.machine.model.MachineModel;
import replicatorg.machine.model.ToolModel;
import replicatorg.model.GCodeSource;
import replicatorg.util.Point5d;

/**
 * The MachineController object controls a single machine. It contains a single
 * machine driver object. All machine operations (building, stopping, pausing)
 * are performed asynchronously by a thread maintained by the MachineController;
 * calls to MachineController ordinarily trigger an operation and return
 * immediately.
 * 
 * When the machine is paused, the machine thread waits on notification to the
 * machine thread object.
 * 
 * In general, the machine thread should *not* be interrupted, as this can cause
 * synchronization issues. Interruption should only really happen on hanging
 * connections and shutdown.
 * 
 * @author phooky
 * 
 */
public class Machine implements MachineInterface {

	public enum RequestType {
		// Set up the connection to the machine
		CONNECT, // Establish connection with the target
		DISCONNECT, // Detach from target
		DISCONNECT_REMOTE_BUILD, // Disconnect from a remote build without stopping it.
		RESET, // Reset the driver

		// Start a build
		SIMULATE, // Build to the simulator
		BUILD_DIRECT, // Build in real time on the machine
		BUILD_TO_FILE, // Build, but instruct the machine to save it to the
						// local filesystem
		BUILD_TO_REMOTE_FILE, // Build, but instruct the machine to save it to
								// the machine's filesystem
		BUILD_REMOTE, // Instruct the machine to run a build from it's
						// filesystem

		// Control a build
		PAUSE, // Pause the current build
		UNPAUSE, // Unpause the current build
		STOP_MOTION, // Stop all motion and abort the current build
		STOP_ALL, // Stop everything (motion and actuators) and abort the current build

		// Interactive command
		RUN_COMMAND, // Run a single command on the driver, interleaved with the
						// build.
		
		SHUTDOWN,	// Stop build (disconnect if building remotely), and stop the thread. 
	}

	public enum JobTarget {
		/** No target selected. */
		NONE,
		/** Operations are being simulated. */
		SIMULATOR,
		/** Operations are performed on a physical machine. */
		MACHINE,
		/** Operations are being captured to an SD card on the machine. */
		REMOTE_FILE,
		/** Operations are being captured to a file. */
		FILE
	};

//	// Test idea for a print job: specifies a gcode source and a target
//	class JobInformation {
//		JobTarget target;
//		GCodeSource source;
//
//		public JobInformation(JobTarget target, GCodeSource source) {
//
//		}
//	}

	/**
	 * Get the machine state. This is a snapshot of the state when the method
	 * was called, not a live object.
	 * 
	 * @return a copy of the machine's state object
	 */
	public MachineState getMachineState() {
		return machineThread.getMachineState();
	}

	MachineThread machineThread;
	final MachineCallbackHandler callbackHandler;
	
	// TODO: WTF is this here for.
	// this is the xml config for this machine.
	protected Node machineNode;


	public String getMachineName() {
		return machineThread.getMachineName();
	}

	/**
	 * Creates the machine object.
	 */
	public Machine(Node mNode, MachineCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler; 
		
		machineNode = mNode;
		machineThread = new MachineThread(this, mNode);
		machineThread.start();
	}

	public boolean buildRemote(String remoteName) {
		machineThread.scheduleRequest(new MachineCommand(
				RequestType.BUILD_REMOTE, null, remoteName));
		return true;
	}
	
	// The estimate function now checks for some sources of error
	// needs a way to return failure
	private String message;
	private long numWarnings;
	private long numErrors;
	
	/**
	 * Begin running a job.
	 */
	public boolean buildDirect(GCodeSource source) {
		// start simulator

		// TODO: Re-enable the simulator.
		// if (simulator != null &&
		// Base.preferences.getBoolean("build.showSimulator",false))
		// simulator.createWindow();

		// estimate build time.
		Base.logger.info("Estimating build time and scanning code for errors...");
		
		// reset any old failures/initialize to a failure free state
		numWarnings = 0;
		numErrors = 0;
		message = null;
		
		estimate(source);
		
		if(numErrors > 0)
		{
			JOptionPane.showConfirmDialog(null, 
					new Object[]{"The pre-run check has found some problematic GCode.",
					"This may be a result of trying to run code on a machine other than the one it's\n" +
					"intended for (i.e. running dual headed GCode on a single headed machine).",
					"This message can be turned off from the preferences menu.",
					"\nError 1 of " + numErrors + " (see console for more): " + message,
					"\nErrors must be fixed before this build can be safely run."},
					"GCode Check: Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if(numWarnings > 0)
		{
			int proceed = JOptionPane.showConfirmDialog(null, 
					new Object[]{"The pre-run check has found some potentially problematic GCode.",
					"This may be a result of trying to run code on a machine other than the one it's\n" +
					"intended for (i.e. running dual headed GCode on a single headed machine).",
					"This message can be turned off from the preferences menu.",
					"\nWarning 1 of " + numWarnings + " (see console for more): " + message,
					"\nWould you like to proceed with the build anyway?"},
					"GCode Check: Warning", JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE);
			
			// I think this is the return value for "no"
			if(proceed == 1)
				return false;
		}
		
		
		// do that build!
		Base.logger.info("Beginning build.");

		machineThread.scheduleRequest(new MachineCommand(RequestType.BUILD_DIRECT, source, null));
		
		return true;
	}

	public void simulate(GCodeSource source) {
		// start simulator
		// if (simulator != null)
		// simulator.createWindow();

		// estimate build time.
		Base.logger.info("Estimating build time...");
		estimate(source);

		// do that build!
		Base.logger.info("Beginning simulation.");
		machineThread.scheduleRequest(new MachineCommand(RequestType.SIMULATE,
				source, null));
	}

	// TODO: Spawn a new thread to handle this for us?
	public void estimate(GCodeSource source) {
		if (source == null) {
			return;
		}

		EstimationDriver estimator = new EstimationDriver();
		// TODO: Is this correct?
		estimator.setMachine(machineThread.getModel());
		
		boolean safetyChecks = Base.preferences.getBoolean("build.safetyChecks", true);
		
		int nToolheads = machineThread.getModel().getTools().size();
		Point5d maxRates = machineThread.getModel().getMaximumFeedrates();
		
		Queue<DriverCommand> estimatorQueue = new LinkedList<DriverCommand>();

		GCodeParser estimatorParser = new GCodeParser();
		estimatorParser.init(estimator);

		// run each line through the estimator
		for (String line : source) {
			// TODO: Hooks for plugins to add estimated time?
			estimatorParser.parse(line, estimatorQueue);

			if(safetyChecks)
			{
				GCode gcLine = new GCode(line);
				String s;

				String mainCode = gcLine.getCommand().split(" ")[0];
				if(!("").equals(mainCode) && GCodeEnumeration.getGCode(mainCode) == null)
				{
					s = "Unsupported GCode!\n" + line + 
							" uses a code that ReplicatorG doesn't recognize.";
					
					//only take the first message
					if(message == null)
						message = s + '\n';

					Base.logger.log(Level.SEVERE, s);
					numErrors++;
				}
				
				// we're going to check for the correct number of toolheads in each command
				// the list of exceptions keeps growing, do we really need to do this check?
				// maybe we should just specify the things to check, rather than the reverse
				if(gcLine.getCodeValue('T') > nToolheads-1 && gcLine.getCodeValue('M') != 109
														   && gcLine.getCodeValue('M') != 106
														   && gcLine.getCodeValue('M') != 107)
				{
					s = "Too Many Toolheads!\n" + line + 
							" makes reference to a non-existent toolhead.";
					
					//only take the first message
					if(message == null)
						message = s + '\n';
					
					Base.logger.log(Level.SEVERE, s);
					numErrors++;
				}
				if(gcLine.hasCode('F'))
				{
					double fVal = gcLine.getCodeValue('F');
					if( (gcLine.hasCode('X') && fVal > maxRates.x()) ||
						(gcLine.hasCode('Y') && fVal > maxRates.y()) ||
	// we're going to ignore this for now, since most of the time the z isn't actually moving 
	//					(gcLine.hasCode('Z') && fVal > maxRates.z()) ||  
						(gcLine.hasCode('A') && fVal > maxRates.a()) ||
						(gcLine.hasCode('B') && fVal > maxRates.b()))
					{
						s = "You're moving too fast!\n" +
								 line + " Tries to turn an axis faster than its max rate.";
						
						//only take the first message
						if(message == null)
							message = s + '\n';
						
						Base.logger.log(Level.WARNING, s);
						numWarnings++;
					}
				}
			}
			
			for (DriverCommand command : estimatorQueue) {
				try {
					command.run(estimator);
				} catch (RetryException r) {
					// Ignore.
				} catch (StopException e) {
					// TODO: Should we stop the estimator when we get a stop???
				}
			}
			estimatorQueue.clear();
		}

		// TODO: Set simulator up properly.
		// if (simulator != null) {
		// simulator.setSimulationBounds(estimator.getBounds());
		// }
		// // oh, how this needs to be cleaned up...
		// if (driver instanceof SimulationDriver) {
		// ((SimulationDriver)driver).setSimulationBounds(estimator.getBounds());
		// }

		machineThread.setEstimatedBuildTime(estimator.getBuildTime());
		Base.logger
				.info("Estimated build time is: "
						+ EstimationDriver.getBuildTimeString(estimator
								.getBuildTime()));
	}

	public DriverQueryInterface getDriverQueryInterface() {
		return (DriverQueryInterface) machineThread.getDriver();
	}

	public Driver getDriver() {
		return machineThread.getDriver();
	}

	public SimulationDriver getSimulatorDriver() {
		return machineThread.getSimulator();
	}

	public MachineModel getModel() {
		return machineThread.getModel();
	}

	public void stopMotion() {
		machineThread.scheduleRequest(new MachineCommand(RequestType.STOP_MOTION,
				null, null));
	}
	
	public void stopAll() {
		machineThread.scheduleRequest(new MachineCommand(RequestType.STOP_ALL,
				null, null));
	}

	synchronized public boolean isConnected() {
		return machineThread.isConnected();
	}

	public void pause() {
		machineThread.scheduleRequest(new MachineCommand(RequestType.PAUSE,
				null, null));
	}

	public void upload(GCodeSource source, String remoteName) {
		/**
		 * Upload the gcode to the given remote SD name.
		 * 
		 * @param source
		 * @param remoteName
		 */
		machineThread.scheduleRequest(new MachineCommand(
				RequestType.BUILD_TO_REMOTE_FILE, source, remoteName));
	}

	public void buildToFile(GCodeSource source, String path) {
		/**
		 * Upload the gcode to the given file.
		 * 
		 * @param source
		 * @param remoteName
		 */
		machineThread.scheduleRequest(new MachineCommand(
				RequestType.BUILD_TO_FILE, source, path));
	}

	public void unpause() {
		machineThread.scheduleRequest(new MachineCommand(RequestType.UNPAUSE,
				null, null));
	}

	public void reset() {
		machineThread.scheduleRequest(new MachineCommand(RequestType.RESET,
				null, null));
	}

	// TODO: make this more generic to handle non-serial connections.
	public void connect(String portName) {
		// recreate thread if stopped
		// TODO: Evaluate this!
		if (!machineThread.isAlive()) {
			machineThread = new MachineThread(this, machineNode);
			machineThread.start();
		}
		
		machineThread.scheduleRequest(new MachineCommand(RequestType.CONNECT,
				null, portName));
	}

	synchronized public void disconnect() {
		machineThread.scheduleRequest(new MachineCommand(
				RequestType.DISCONNECT, null, null));
	}

	synchronized public boolean isPaused() {
		return getMachineState().isPaused();
	}

	public void runCommand(DriverCommand command) {
		machineThread.scheduleRequest(new MachineCommand(
				RequestType.RUN_COMMAND, command));
	}

	public void dispose() {
		if (machineThread != null) {
			machineThread.scheduleRequest(new MachineCommand(
					RequestType.SHUTDOWN, null, null));

			// Wait 5 seconds for the thread to stop.
			try {
				machineThread.join(5000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void emitStateChange(MachineState current, String message) {
		MachineStateChangeEvent e = new MachineStateChangeEvent(this, current, message);
		
		callbackHandler.schedule(e);
	}

	protected void emitProgress(MachineProgressEvent progress) {
		callbackHandler.schedule(progress);
	}

	protected void emitToolStatus(ToolModel tool) {
		MachineToolStatusEvent e = new MachineToolStatusEvent(this, tool);
		callbackHandler.schedule(e);
	}

	
	public int getLinesProcessed() {
		/*
		 * This is for jumping to the right line when aborting or pausing. This
		 * way you'll have the ability to track down where to continue printing.
		 */
		return machineThread.getLinesProcessed();
	}

	// TODO: Drop this
	public boolean isSimulating() {
		return machineThread.isSimulating();
	}

	// TODO: Drop this
	public boolean isInteractiveTarget() {
		return machineThread.isInteractiveTarget();
	}

	// TODO: Drop this
	public JobTarget getTarget() {
		return machineThread.getTarget();
	}
}
