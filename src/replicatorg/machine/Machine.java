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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.gcode.GCodeCommand;
import replicatorg.app.gcode.GCodeEnumeration;
import replicatorg.app.gcode.GCodeParser;
import replicatorg.drivers.Driver;
import replicatorg.drivers.DriverQueryInterface;
import replicatorg.drivers.EstimationDriver;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SimulationDriver;
import replicatorg.drivers.StopException;
import replicatorg.drivers.commands.DriverCommand;
import replicatorg.machine.MachineState.State;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.Endstops;
import replicatorg.machine.model.MachineModel;
import replicatorg.machine.model.MachineType;
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

		/// set initial state to propigate new machine info via callbacks
		machineThread.scheduleRequest(new MachineCommand(
				RequestType.DISCONNECT, null, null));

	}

	public boolean buildRemote(String remoteName) {
		machineThread.scheduleRequest(new MachineCommand(
				RequestType.BUILD_REMOTE, null, remoteName));
		return true;
	}
	
	/**
	 * Begin running a job.
	 */
	@Override
	public void buildDirect(final GCodeSource source) {

		Runnable prepareAndStart = new Runnable(){

			private Map<String, Integer> messages = new TreeMap<String, Integer>();
			private boolean cancelled = false;
			@Override
			public void run() {

				// start simulator

				// TODO: Re-enable the simulator.
				// if (simulator != null &&
				// Base.preferences.getBoolean("build.showSimulator",false))
				// simulator.createWindow();
				
				Base.logger.info("Estimating build time and scanning code for errors...");
				
				if(Base.preferences.getBoolean("build.safetyChecks", true))
				{
					emitStateChange(new MachineState(State.BUILDING), "Running safety checks...");
					
					safetyCheck(source, messages);

					if(! messages.isEmpty())
					{
						System.out.println("errors");
						final JPanel displayPanel = new JPanel(new MigLayout("fill"));
						final JDialog dialog = new JDialog(Base.getEditor(), "GCode warning", true);
//						
//						displayPanel.add(new JLabel("<html>The pre-run check has found some potentially problematic GCode.<br/>" +
//								"This may be a result of trying to run code on a machine other than the one it's<br/>" +
//								"intended for (i.e. running dual headed GCode on a single headed machine).</html>"), "growx, wrap");
						JTextArea testLabel = new JTextArea();
						testLabel.setLineWrap(true);
						testLabel.setWrapStyleWord(true);
						testLabel.setEditable(false);
						testLabel.setOpaque(false);
						testLabel.setBorder(BorderFactory.createEmptyBorder());
						testLabel.setFont(new JLabel().getFont());
						testLabel.setText("The pre-run check has found some potentially problematic GCode. This may be a result of trying" +
								" to run code on a machine other than the one it's intended for (i.e. running dual headed GCode on a " +
								"single headed machine).\n\nClick on a message to see the last place it occurred.");
						displayPanel.add(testLabel, "growx, wrap");
						
						final JPanel messagePanel = new JPanel(new MigLayout("fill, ins 0"));
						
						List<String> displayMessages = new ArrayList<String>(messages.keySet());
						if(displayMessages.size() > 10)
						{
							String moreMessage = "And " + (displayMessages.size()-10) + " more...";
							displayMessages = displayMessages.subList(0, 10);
							displayMessages.add(moreMessage);
						}
						final JList messageList = new JList(displayMessages.toArray());
						
						messageList.addMouseListener(new MouseAdapter(){
							@Override
							public void mouseClicked(MouseEvent arg0) {
								if(arg0.getClickCount() == 1)
									highlightLine(messageList.getSelectedValue());
							}
						});
						/// do initial highlight and selection of default item (the 0th)
						messageList.setSelectedIndex(0);
						highlightLine(displayMessages.get(0));
						
						messageList.addKeyListener(new KeyAdapter(){
							@Override
							public void keyPressed(KeyEvent arg0) {
								if(arg0.getKeyCode() == KeyEvent.VK_ENTER)
								{
									highlightLine(messageList.getSelectedValue());
								} else if(arg0.getKeyCode() == KeyEvent.VK_UP) {
									messageList.setSelectedIndex(Math.max(messageList.getSelectedIndex(), 0));
								} else if(arg0.getKeyCode() == KeyEvent.VK_DOWN) {
									messageList.setSelectedIndex(Math.min(messageList.getSelectedIndex(), messageList.getModel().getSize()));
								}
							}
						});
						
						messagePanel.add(messageList, "growx, growy");
						displayPanel.add(new JScrollPane(messagePanel), "growx, growy, wrap");
						
						JButton proceedButton = new JButton("Proceed anyway");
						proceedButton.addActionListener(new ActionListener(){
							@Override
							public void actionPerformed(ActionEvent arg0) {
								dialog.dispose();
							}
						});
						displayPanel.add(proceedButton, "align right, split");
	
						JButton cancelButton = new JButton("Cancel build");
						cancelButton.addActionListener(new ActionListener(){
							@Override
							public void actionPerformed(ActionEvent arg0) {
								cancelled = true;
								// TRICKY:
								// see machine thread for a full explanation of this.
								// basically, we need to get mainwindow to forget it was printing
								boolean connected = getMachineState().canPrint();
								emitStateChange(new MachineState(State.ERROR), "Print cancelled");
								emitStateChange(new MachineState(State.NOT_ATTACHED), "Print cancelled");
								if(connected)
									emitStateChange(new MachineState(State.READY), "Print cancelled");
								dialog.dispose();
							}
						});
						displayPanel.add(cancelButton, "align right, wrap");
						
						dialog.add(displayPanel);
						dialog.pack();
						dialog.setVisible(true);
					}
				}

				if(!cancelled)
				{
					// estimate build time.
					emitStateChange(new MachineState(State.BUILDING), "Estimating time to completion...");
					estimate(source);
					
					// do that build!
					Base.logger.info("Beginning build.");
	
					machineThread.scheduleRequest(new MachineCommand(RequestType.BUILD_DIRECT, source, null));
				}
			}
			
			private void highlightLine(Object atWhichLine)
			{
				Base.getEditor().highlightLine(messages.get(atWhichLine));
			}
		};
		Executors.newSingleThreadExecutor().execute(prepareAndStart);
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

	public void safetyCheck(GCodeSource source, Map<String, Integer> messages)
	{
		int nToolheads = machineThread.getModel().getTools().size();
		Point5d maxRates = machineThread.getModel().getMaximumFeedrates();
		
//		BuildVolume buildVolume = new BuildVolume();
//		buildVolume.setX(machineThread.getModel().getBuildVolume().getX()/2);
//		buildVolume.setY(machineThread.getModel().getBuildVolume().getY()/2);
//		buildVolume.setZ(machineThread.getModel().getBuildVolume().getZ()/2);
		
		GCodeCommand gcode;
		String message, cmd, mainCode;
		Integer lineNumber = 0;
		
		for(String line : source)
		{
			try
			{
				gcode = new GCodeCommand(line);
			} //Catching every kind of exception is generally bad form,
			//  It can hide where the problem is happening, and should be avoided
			//  But I'm doing it anyway.
			catch(Exception e)
			{
				message = "ReplicatorG can't parse '" + line +"'";

				messages.put(message, lineNumber);
				Base.logger.log(Level.SEVERE, message);
				continue;
			}

			cmd = gcode.getCommand();
			if(cmd.split(" ").length < 1) continue; //to avoid null index problems
			
			mainCode = cmd.split(" ")[0];

			if(!("").equals(mainCode) && GCodeEnumeration.getGCode(mainCode) == null)
			{
				message = "ReplicatorG doesn't recognize GCode '" + line +"'";

				messages.put(message, lineNumber);
				Base.logger.log(Level.SEVERE, message);
			}
			
			// Check for homing in the wrong direction
			if(!homingDirectionIsSafe(gcode))
			{
				message = "Homing in the wrong direction can damage your machine: '" + line +"'";

				messages.put(message, lineNumber);
				Base.logger.log(Level.SEVERE, message);
			}

			// we're going to check for the correct number of toolheads in each command
			// the list of exceptions keeps growing, do we really need to do this check?
			// maybe we should just specify the things to check, rather than the reverse
			if(gcode.getCodeValue('T') > nToolheads-1 && gcode.getCodeValue('M') != 109
													   && gcode.getCodeValue('M') != 106
													   && gcode.getCodeValue('M') != 107)
			{
				message = "Too Many Toolheads! You don't have a toolhead numbered " + gcode.getCodeValue('T');
				
				messages.put(message, lineNumber);
				Base.logger.log(Level.SEVERE, message);
				/// this error may happen a TON of times in a big model, in this case exit on the first instance.
				// I disagree. - Ted
//				return; 
			}
			if(gcode.hasCode('F'))
			{
				double fVal = gcode.getCodeValue('F');
				if( (gcode.hasCode('X') && fVal > maxRates.x()) ||
					(gcode.hasCode('Y') && fVal > maxRates.y()) ||
// we're going to ignore this for now, since most of the time the z isn't actually moving 
//					(gcLine.hasCode('Z') && fVal > maxRates.z()) ||  
					(gcode.hasCode('A') && fVal > maxRates.a()) ||
					(gcode.hasCode('B') && fVal > maxRates.b()))
				{
					message = "You're moving too fast! " + line +
							 " turns at least one axis faster than its max speed.";

					messages.put(message, lineNumber);
					Base.logger.log(Level.WARNING, message);
				}
			}
			
			lineNumber++;
		}
	}
	
	private boolean homingDirectionIsSafe(GCodeCommand gcode) {
		Endstops xstop, ystop, zstop;
		
		// If it doesn't have the code, ignore it
		xstop = ystop = zstop = Endstops.BOTH;
		
		if(gcode.hasCode('X'))
			xstop = machineThread.getModel().getEndstops(AxisId.X);
		if(gcode.hasCode('Y'))
			ystop = machineThread.getModel().getEndstops(AxisId.Y);
		if(gcode.hasCode('Z'))
			zstop = machineThread.getModel().getEndstops(AxisId.Z);
		
		if(gcode.getCodeValue('G') == 161)
		{
			if((xstop != Endstops.MIN) && (xstop != Endstops.BOTH))
				return false;
			if((ystop != Endstops.MIN) && (ystop != Endstops.BOTH))
				return false;
			if((zstop != Endstops.MIN) && (zstop != Endstops.BOTH))
				return false;
		}
		else if(gcode.getCodeValue('G') == 162)
		{
			if((xstop != Endstops.MAX) && (xstop != Endstops.BOTH))
				return false;
			if((ystop != Endstops.MAX) && (ystop != Endstops.BOTH))
				return false;
			if((zstop != Endstops.MAX) && (zstop != Endstops.BOTH))
				return false;
		}
		return true;
	}
	
	// TODO: Spawn a new thread to handle this for us?
	public void estimate(GCodeSource source) {
		if (source == null) {
			return;
		}

		EstimationDriver estimator = new EstimationDriver();
		// TODO: Is this correct?
		estimator.setMachine(machineThread.getModel());
		
		Queue<DriverCommand> estimatorQueue = new LinkedList<DriverCommand>();

		GCodeParser estimatorParser = new GCodeParser();
		estimatorParser.init(estimator);

		// run each line through the estimator
		for (String line : source) {
			// TODO: Hooks for plugins to add estimated time?
			estimatorParser.parse(line, estimatorQueue);
			
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
		Base.logger.info("Estimated build time is: " + 
					EstimationDriver.getBuildTimeString(estimator.getBuildTime()));
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
	
	@Override
	public MachineType getMachineType()
	{
		return getModel().getMachineType();
	}
}
