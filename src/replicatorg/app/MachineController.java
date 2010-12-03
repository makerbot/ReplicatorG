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

package replicatorg.app;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.exceptions.BuildFailureException;
import replicatorg.app.exceptions.GCodeException;
import replicatorg.app.exceptions.JobCancelledException;
import replicatorg.app.exceptions.JobEndException;
import replicatorg.app.exceptions.JobException;
import replicatorg.app.exceptions.JobRewindException;
import replicatorg.app.tools.XML;
import replicatorg.app.ui.MainWindow;
import replicatorg.drivers.Driver;
import replicatorg.drivers.DriverFactory;
import replicatorg.drivers.EstimationDriver;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SDCardCapture;
import replicatorg.drivers.SimulationDriver;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;
import replicatorg.machine.model.MachineModel;
import replicatorg.machine.model.ToolModel;
import replicatorg.model.GCodeSource;
import replicatorg.model.StringListSource;

/**
 * The MachineController object controls a single machine. It contains a single
 * machine driver object. All machine operations (building, stopping, pausing)
 * are performed asynchronously by a thread maintained by the MachineController;
 * calls to MachineController ordinarily trigger an operation and return
 * immediately.
 * 
 * When the machine is paused, the machine thread waits on notification to the machine thread object.
 * 
 * In general, the machine thread should *not* be interrupted, as this can cause synchronization issues.
 * Interruption should only really happen on hanging connections and shutdown.
 * 
 * @author phooky
 * 
 */
public class MachineController {

	private MachineState state = new MachineState();
	/**
	 * Get the machine state.  This is a snapshot of the state when the method was called, not a live object.
	 * @return a copy of the machine's state object
	 */
	public MachineState getMachineState() { return state.clone(); }
	
	/**
	 * Set the a machine state.  If the state is not the current state, a state change
	 * event will be emitted and the machine thread will be notified.  
	 * @param state the new state of the machine.
	 */
	private void setState(MachineState state) {
		MachineState oldState = this.state;
		this.state = state;
		if (!oldState.equals(state)) {
			emitStateChange(oldState,state);
			// wake up machine thread
			synchronized(machineThread) {
				machineThread.notify(); // wake up paused machines
			}
		}
	}

	/**
	 * A helper for setting the machine state to a simple state.
	 * @param state The new state.
	 */
	private void setState(MachineState.State state) {
		MachineState newState = getMachineState();
		newState.setState(state);
		setState(newState);
	}

	// Build statistics
	private int linesProcessed = -1;
	private int linesTotal = -1;
	private double startTimeMillis = -1;
	
	/**
	 * The MachineThread is responsible for communicating with the machine.
	 * 
	 * @author phooky
	 * 
	 */
	class MachineThread extends Thread {
		private long lastPolled = 0;
		private boolean pollingEnabled = false;
		private long pollIntervalMs = 1000;

		/**
		 * Start polling the machine for its current status (temperatures, etc.)
		 * @param interval The interval, in ms, between polls
		 */
		synchronized void startStatusPolling(long interval) {
			pollingEnabled = true;
			pollIntervalMs = interval;
		}

		/**
		 * Turn off status polling.
		 */
		synchronized void stopStatusPolling() {
			pollingEnabled = false;
		}
		
		/**
		 * Run the warmup commands.
		 * 
		 * @throws BuildFailureException
		 * @throws InterruptedException
		 */
		private void runWarmupCommands() throws BuildFailureException, InterruptedException {
			Base.logger.info("Running warmup commands.");
			buildCodesInternal(new StringListSource(warmupCommands));
		}

		private void runCooldownCommands() throws BuildFailureException, InterruptedException {
			Base.logger.info("Running cooldown commands.");
			buildCodesInternal(new StringListSource(cooldownCommands));
		}
		
		// Interrupt the machine controller thread.
		// The driver implementation should be smart enough to check for interrupted status when not blocking
		// on IO, and handle interrupt exceptions by dumping out.
		public void interruptDriver() {
			this.interrupt();
		}

		/**
		 * Build the provided gcodes.  This method does not return until the build is complete or has been terminated.
		 * The build target need not be an actual machine; it can be a file as well.  An "upload" is considered a build
		 * to a machine.  
		 * @param source The gcode to build.
		 * @return true if build terminated normally
		 * @throws BuildFailureException
		 * @throws InterruptedException
		 */
		private boolean buildCodesInternal(GCodeSource source) throws BuildFailureException, InterruptedException {
			if (!state.isBuilding()) {
				// Do not build if the machine is not building or paused
				return false;
			}
			
			// Flush any parser cached data
			driver.getParser().init(driver);
			
			Iterator<String> i = source.iterator();
			boolean retry = false;
			// Iterate over all the lines in the gcode source.
			while (i.hasNext()) {
				if (retry == false) {
					String line = i.next();
					linesProcessed++;
					if (Thread.currentThread().isInterrupted()) {
						throw new BuildFailureException("Build was interrupted");
					}
					
					if (simulator.isSimulating()) {
						// Parse a line for the simulator
						simulator.parse(line);
					}
					if (!state.isSimulating()) {
						// Parse a line for the actual machine
						driver.parse(line); 
					}
				}
				try {
					// Check if there are any interactive stops on this line; if so,
					// wait for user response.
					GCodeParser.StopInfo info = driver.getParser().getStops();
					if (info != null &&
							Base.preferences.getBoolean("machine.optionalstops",true) &&
							state.isBuilding() &&
							state.isInteractiveTarget()) {
						JobException e = info.getException(); 
						if (info.isOptional()) {
							int result = JOptionPane.showConfirmDialog(null, info.getMessage(),
									"Continue Build?", JOptionPane.YES_NO_OPTION);
							if (result != JOptionPane.YES_OPTION) {
								e = info.getCancelException();
							}
						} else {
							JOptionPane.showMessageDialog(null, info.getMessage(), 
									"Build stop", JOptionPane.INFORMATION_MESSAGE);
						}
						if (e != null) {
							throw e;
						}
					}
				} catch (JobEndException e) {
					// M2 codes indicate job done
					return true;
				} catch (JobCancelledException e) {
					throw new BuildFailureException("Job cancelled by user.");
				} catch (JobRewindException e) {
					// Rewind the job to start of source
					i = source.iterator();
					continue;
				} catch (JobException e) {
					Base.logger.severe("Unknown job exception emitted");
				}
				
				// simulate the command.
				if (retry == false && simulator.isSimulating()) {
					try {
						simulator.execute();
					} catch (RetryException r) {
						// Ignore.
					}
				}
				
				
				try {
					if (!state.isSimulating()) {
						// Run the command on the machine.
						driver.execute();
					}
					retry = false;
				} catch (RetryException r) {
					// Indicate that we should retry the current line, rather
					// than proceeding to the next, on the next go-round.
					retry = true;
				}
				catch (GCodeException e) {
					// This is severe, but not fatal; ordinarily it means there's an
					// unrecognized gcode in the source.
					Base.logger.severe("Error: " + e.getMessage());
				} catch (InterruptedException ie) {
					// We're in the middle of a stop or shutdown
				}
				
				// did we get any errors?
				if (!state.isSimulating()) {
					driver.checkErrors();
				}
				
				// are we paused?
				if (state.isPaused()) {
					// Tell machine to enter pause mode
					if (!state.isSimulating()) driver.pause();
					while (state.isPaused()) {
						// Sleep until notified
						synchronized(this) { wait(); }
					}
					// Notified; tell machine to wake up.
					if (!state.isSimulating()) driver.unpause();
				}
				
				// Send a stop command if we're stopping.
				if (state.getState() == MachineState.State.STOPPING ||
						state.getState() == MachineState.State.RESET) {
					if (!state.isSimulating()) {
						driver.stop();
					}
					throw new BuildFailureException("Build manually aborted");
				}

				// bail if we're no longer building
				if (state.getState() != MachineState.State.BUILDING) {
					return false;
				}
				
				// send out updates
				if (pollingEnabled) {
					long curMillis = System.currentTimeMillis();
					if (lastPolled + pollIntervalMs <= curMillis) {
						lastPolled = curMillis;
						pollStatus();
					}
				}
				MachineProgressEvent progress = 
					new MachineProgressEvent((double)System.currentTimeMillis()-startTimeMillis,
							estimatedBuildTime,
							linesProcessed,
							linesTotal);
				emitProgress(progress);
			}
			
			// wait for driver to finish up.
			if (!state.isSimulating()) while (!driver.isFinished()) {
				Thread.sleep(100);
			}
			return true;
		}

		public boolean isReady() { return state.isReady(); }

		public void pollStatus() {
			if (state.isBuilding() && !state.isSimulating()) {
				if (Base.preferences.getBoolean("build.monitor_temp",false)) {
					driver.readTemperature();
					emitToolStatus(driver.getMachine().currentTool());
				}
			}
		}
		
		// Enter the reset state
		public void reset() {
			if (state.isConnected()) {
				setState(new MachineState(MachineState.State.RESET));
			}
		}
		
		// Begin connecting to the machine
		public void connect() {
			setState(new MachineState(MachineState.State.CONNECTING));
		}

		GCodeSource currentSource;
		
		// Build the gcode source, bracketing it with warmup and cooldown commands.
		// 
		private void buildInternal(GCodeSource source) {
			startTimeMillis = System.currentTimeMillis();
			linesProcessed = 0;
			linesTotal = warmupCommands.size() + 
				cooldownCommands.size() +
				source.getLineCount();
			startStatusPolling(1000); // Will not send commands if temp mon. turned off
			try {
				if (!state.isSimulating()) {
					driver.getCurrentPosition(); // reconcile position
				}
				runWarmupCommands();
				Base.logger.info("Running build.");
				buildCodesInternal(source);
				runCooldownCommands();
				if (!state.isSimulating()) {
					driver.invalidatePosition();
				}
				setState(new MachineState(driver.isInitialized()?
						MachineState.State.READY:
						MachineState.State.NOT_ATTACHED
					));
			} catch (BuildFailureException e) {
				if (state.isSimulating()) {
					// If simulating, return to connected or
					// disconnected state.
					setState(new MachineState(driver.isInitialized()?
							MachineState.State.READY:
							MachineState.State.NOT_ATTACHED));
				} else {
					// If a real interrupted build,
					// Attempt to reestablish connection to check state on an abort
					// or failure
					setState(new MachineState(MachineState.State.CONNECTING));
				}
			} catch (InterruptedException e) {
				Base.logger.warning("MachineController interrupted");
			} finally {
				stopStatusPolling();
			}
		}

		String remoteName = null;
		
		// Run a remote SD card build on the machine.
		private void buildRemoteInternal(String remoteName) {
			// Dump out if SD builds are unsupported on this machine
			if (remoteName == null || !(driver instanceof SDCardCapture)) return;
			if (state.getState() != MachineState.State.PLAYBACK) return;
			driver.getCurrentPosition(); // reconcile position
			SDCardCapture sdcc = (SDCardCapture)driver;
			if (!processSDResponse(sdcc.playback(remoteName))) {
				setState(MachineState.State.STOPPING);
				return;
			}
			// Poll for completion until done.  Check for pause states as well.
			while (running && !driver.isFinished()) {
				try {
					// are we paused?
					if (state.isPaused()) {
						driver.pause();
						while (state.isPaused()) {
							synchronized(this) { wait(); }
						}
						driver.unpause();
					}

					// bail if we got interrupted.
					if (state.getState() != MachineState.State.PLAYBACK) return;
					synchronized(this) { wait(1000); }// wait one second.  A pause will notify us to check the pause state.
				} catch (InterruptedException e) {
					// bail if we got interrupted.
					if (state.getState() != MachineState.State.PLAYBACK) return;
				}
			}
			driver.invalidatePosition();
			setState(new MachineState(MachineState.State.READY));
		}
		
		/**
		 * Run an ordinary gcode build directly on the machine.
		 * @param source
		 */
		public void build(GCodeSource source) {
			currentSource = source;
			setState(new MachineState(MachineState.State.BUILDING,MachineState.Target.MACHINE));
		}
		
		/**
		 * Simulate a gcode build without running it on the machine.
		 * @param source
		 */
		public void simulate(GCodeSource source) {
			currentSource = source;
			setState(new MachineState(MachineState.State.BUILDING,MachineState.Target.SIMULATOR));
		}

		/**
		 * Upload the gcode to the given remote SD name.
		 * @param source
		 * @param remoteName
		 */
		public void upload(GCodeSource source, String remoteName) {
			currentSource = source;
			this.remoteName = remoteName;
			setState(new MachineState(MachineState.State.BUILDING,MachineState.Target.SD_UPLOAD));
		}

		/**
		 * Build to the given file.
		 * @param source
		 * @param path
		 */
		public void buildToFile(GCodeSource source, String path) {
			currentSource = source;
			this.remoteName = path;
			setState(new MachineState(MachineState.State.BUILDING,MachineState.Target.FILE));
		}

		/**
		 * Run the remote build with the given filename.
		 * @param remoteName
		 */
		public void buildRemote(String remoteName) {
			this.remoteName = remoteName;
			setState(MachineState.State.PLAYBACK);
		}
		
		/**
		 * Pause a running build.
		 */
		public void pauseBuild() {
			if (state.isBuilding() && !state.isPaused()) {
				MachineState newState = getMachineState();
				newState.setPaused(true);
				setState(newState);
			}
		}
		
		/**
		 * Resume a paused build.
		 */
		public void resumeBuild() {
			if (state.isBuilding() && state.isPaused()) {
				MachineState newState = getMachineState();
				newState.setPaused(false);
				setState(newState);
			}
		}
		
		/**
		 * Stop a running build.
		 */
		public void stopBuild() {
			driver.getMachine().currentTool().setTargetTemperature(0);
			driver.getMachine().currentTool().setPlatformTargetTemperature(0);
			if (state.isBuilding()) {
				setState(MachineState.State.STOPPING);
			}
		}
		
		/**
		 * Shutdown: abort the running build IF the current build is not an SD build.
		 * SD builds are allowed to continue.
		 */
		public void shutdown() {
			if (state.getState() == MachineState.State.PLAYBACK) {
				running = false;
				return; // send no further packets to machine; let it go on its own
			}
			
			if (state.isBuilding()) {
				setState(MachineState.State.STOPPING);
			}
			running = false;
			synchronized(this) { notify(); }
		}

		private boolean running = true;
		
		protected boolean isRunning() {
			// If we're in stopping mode, we don't want to terminate until the stop
			// packet is sent!
			return running ||
				state.getState() == MachineState.State.STOPPING;
		}
		/**
		 * Main machine thread loop.
		 */
		public void run() {
			while (isRunning()) {
				try {
					if (state.getState() == MachineState.State.BUILDING) {
						// Capture build to a card
						if (state.getTarget() == MachineState.Target.SD_UPLOAD) {
							if (driver instanceof SDCardCapture) {
								SDCardCapture sdcc = (SDCardCapture)driver;
								if (processSDResponse(sdcc.beginCapture(remoteName))) { 
									buildInternal(currentSource);
									Base.logger.info("Captured bytes: " +Integer.toString(sdcc.endCapture()));
								} else { setState(MachineState.State.STOPPING); }
							} else {
								setState(MachineState.State.STOPPING);
							}
						} else if (state.getTarget() == MachineState.Target.FILE) {
							// Output build to a file
							if (driver instanceof SDCardCapture) {
								SDCardCapture sdcc = (SDCardCapture)driver;
								try {
									sdcc.beginFileCapture(remoteName); 
									buildInternal(currentSource);
									sdcc.endFileCapture();
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else {
								setState(MachineState.State.STOPPING);
							}
						} else {
							// Ordinary build
							buildInternal(currentSource);
						}
					} else if (state.getState() == MachineState.State.PLAYBACK) {
						buildRemoteInternal(remoteName);
					} else if (state.getState() == MachineState.State.CONNECTING) {
						driver.initialize();
						if (driver.isInitialized()) {
							readName();
							setState(MachineState.State.READY);
						} else {
							setState(MachineState.State.NOT_ATTACHED);
						}
					} else if (state.getState() == MachineState.State.STOPPING) {
						driver.stop();
						setState(MachineState.State.READY);						
					} else if (state.getState() == MachineState.State.RESET) {
						driver.reset();
						setState(MachineState.State.READY);						
					} else {
						synchronized(this) {
							if (state.getState() == MachineState.State.READY ||
									state.getState() == MachineState.State.NOT_ATTACHED ||
									state.isPaused()) {
								wait();
							} else {
							}
						}
					}
				} catch (InterruptedException ie) {
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void readName() {
		if (driver instanceof OnboardParameters) {
			String n = ((OnboardParameters)driver).getMachineName();
			if (n != null && n.length() > 0) {
				name = n;
			}
		}
	}
	MachineThread machineThread = new MachineThread();
	
	// The GCode source of the current build source.
	protected GCodeSource source;
	
	// this is the xml config for this machine.
	protected Node machineNode;

	// The name of our machine.
	protected String name;

	public String getName() { return name; }
	
	// Our driver object. Null when no driver is selected.
	public Driver driver = null;
	
	// the simulator driver
	protected SimulationDriver simulator;

	// our current thread.
	protected Thread thread;
	
	// estimated build time in millis
	protected double estimatedBuildTime = 0;

	// our warmup/cooldown commands
	protected Vector<String> warmupCommands;

	protected Vector<String> cooldownCommands;

	/**
	 * Creates the machine object.
	 */
	public MachineController(Node mNode) {
		// save our XML
		machineNode = mNode;

		parseName();
		Base.logger.info("Loading machine: " + name);

		// load our various objects
		loadDriver();
		loadExtraPrefs();
		machineThread = new MachineThread();
		machineThread.start();
	}

	public void setCodeSource(GCodeSource source) {
		this.source = source;
	}

	// TODO: hide this behind an API
	private MainWindow window; // for responses to errors, etc.
	public void setMainWindow(MainWindow window) { this.window = window; }

	static Map<SDCardCapture.ResponseCode,String> sdErrorMap =
		new EnumMap<SDCardCapture.ResponseCode,String>(SDCardCapture.ResponseCode.class);
	{
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_NO_CARD,
				"No SD card was detected.  Please make sure you have a working, formatted\n" +
				"SD card in the motherboard's SD slot and try again.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_INIT,
				"ReplicatorG was unable to initialize the SD card.  Please make sure that\n" +
				"the SD card works properly.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_PARTITION,
				"ReplicatorG was unable to read the SD card's partition table.  Please check\n" +
				"that the card is partitioned properly.\n" +
				"If you believe your SD card is OK, try resetting your device and restarting\n" +
				"ReplicatorG."
				);
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_FS,
				"ReplicatorG was unable to open the filesystem on the SD card.  Please make sure\n" +
				"that the SD card has a single partition formatted with a FAT16 filesystem.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_ROOT_DIR,
				"ReplicatorG was unable to read the root directory on the SD card.  Please\n"+
				"check to see if the SD card was formatted properly.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_LOCKED,
				"The SD card cannot be written to because it is locked.  Remove the card,\n" +
				"switch the lock off, and try again.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_NO_FILE,
				"ReplicatorG could not find the build file on the SD card.");
		sdErrorMap.put(SDCardCapture.ResponseCode.FAIL_GENERIC,"Unknown SD card error.");
	}
	
	/**
	 * Process an SD response code and throw up an appropriate dialog for the user.
	 * @param code the response from the SD request
	 * @return true if the code indicates success; false if the operation should be aborted
	 */
	public boolean processSDResponse(SDCardCapture.ResponseCode code) {
		if (code == SDCardCapture.ResponseCode.SUCCESS) return true;
		String message = sdErrorMap.get(code);
		JOptionPane.showMessageDialog(
				window,
				message,
				"SD card error",
				JOptionPane.ERROR_MESSAGE);
		return false;
	}

	private String descriptorName;
	
	public String getDescriptorName() { return descriptorName; }
	
	private void parseName() {
		NodeList kids = machineNode.getChildNodes();

		for (int j = 0; j < kids.getLength(); j++) {
			Node kid = kids.item(j);

			if (kid.getNodeName().equals("name")) {
				descriptorName = kid.getFirstChild().getNodeValue().trim();
				name = descriptorName;
				return;
			}
		}

		name = "Unknown";
	}

	public boolean buildRemote(String remoteName) {
		machineThread.buildRemote(remoteName);
		return true;
	}
	
	/**
	 * Begin running a job.
	 */
	public boolean execute() {
		// start simulator
		if (simulator != null && Base.preferences.getBoolean("build.showSimulator",false))
			simulator.createWindow();

		// estimate build time.
		Base.logger.info("Estimating build time...");
		estimate();

		// do that build!
		Base.logger.info("Beginning build.");
		machineThread.build(source);
		return true;
	}

	public boolean simulate() {
		// start simulator
		if (simulator != null)
			simulator.createWindow();

		// estimate build time.
		Base.logger.info("Estimating build time...");
		estimate();

		// do that build!
		Base.logger.info("Beginning simulation.");
		machineThread.simulate(source);
		return true;
	}


	public void estimate() {
		if (source == null) { return; }
		try {
			EstimationDriver estimator = new EstimationDriver();
			estimator.setMachine(loadModel());

			// run each line through the estimator
			for (String line : source) {
				// parse only if line is NOT a Twitterbot M code
				if ((line.indexOf(GCodeParser.TB_CODE + Integer.toString(GCodeParser.TB_INIT)) == -1) &&
					(line.indexOf(GCodeParser.TB_CODE + Integer.toString(GCodeParser.TB_MESSAGE)) == -1) &&
					(line.indexOf(GCodeParser.TB_CODE + Integer.toString(GCodeParser.TB_CLEANUP)) == -1)) {
					// use our parser to handle the stuff.
					estimator.parse(line);
					estimator.execute();
				}
			}

			if (simulator != null) {
				simulator.setSimulationBounds(estimator.getBounds());
			}
			// oh, how this needs to be cleaned up...
			if (driver instanceof SimulationDriver) {
				((SimulationDriver)driver).setSimulationBounds(estimator.getBounds());
			}
			estimatedBuildTime = estimator.getBuildTime();
			Base.logger.info("Estimated build time is: "
					+ EstimationDriver.getBuildTimeString(estimatedBuildTime));
		} catch (InterruptedException e) {
			assert (false);
			// Should never happen
		}
	}

	private MachineModel loadModel() {
		MachineModel model = new MachineModel();
		model.loadXML(machineNode);
		return model;
	}
		
	private void loadDriver() {
		// load our utility drivers
		if (Base.preferences.getBoolean("machinecontroller.simulator",true)) {
			Base.logger.info("Loading simulator.");
			simulator = new SimulationDriver();
			simulator.setMachine(loadModel());
		}
		Node driverXml = null; 
		// load our actual driver
		NodeList kids = machineNode.getChildNodes();
		for (int j = 0; j < kids.getLength(); j++) {
			Node kid = kids.item(j);
			if (kid.getNodeName().equals("driver")) {
				driverXml = kid;
			}
		}
		driver = DriverFactory.factory(driverXml);
		driver.setMachine(getModel());
		// Initialization is now handled by the machine thread when it
		// is placed in a connecting state.
	}

	private void loadExtraPrefs() {
		String[] commands = null;
		String command = null;

		warmupCommands = new Vector<String>();
		if (XML.hasChildNode(machineNode, "warmup")) {
			String warmup = XML.getChildNodeValue(machineNode, "warmup");
			commands = warmup.split("\n");

			for (int i = 0; i < commands.length; i++) {
				command = commands[i].trim();
				warmupCommands.add(new String(command));
				// System.out.println("Added warmup: " + command);
			}
		}

		cooldownCommands = new Vector<String>();
		if (XML.hasChildNode(machineNode, "cooldown")) {
			String cooldown = XML.getChildNodeValue(machineNode, "cooldown");
			commands = cooldown.split("\n");

			for (int i = 0; i < commands.length; i++) {
				command = commands[i].trim();
				cooldownCommands.add(new String(command));
				// System.out.println("Added cooldown: " + command);
			}
		}
	}

	public Driver getDriver() {
		return driver;
	}

	public SimulationDriver getSimulatorDriver() {
		return simulator;
	}

	MachineModel cachedModel = null;
	
	public MachineModel getModel() {
		if (cachedModel == null) { cachedModel = loadModel(); }
		return cachedModel;
	}

	synchronized public void stop() {
		machineThread.stopBuild();
	}

	synchronized public boolean isInitialized() {
		return (driver != null && driver.isInitialized());
	}

	synchronized public void pause() {
		machineThread.pauseBuild();
	}

	synchronized public void upload(String remoteName) {
		machineThread.upload(source, remoteName);
	}

	synchronized public void buildToFile(String path) {
		machineThread.buildToFile(source, path);
	}

	
	synchronized public void unpause() {
		machineThread.resumeBuild();
	}

	synchronized public void reset() {
		machineThread.reset();
	}

	synchronized public void connect() {
		// recreate thread if stopped
		if (!machineThread.isAlive()) {
			machineThread = new MachineThread();
			machineThread.start();
		}
		machineThread.connect();
	}

	synchronized public void disconnect() {
		driver.uninitialize();
		setState(new MachineState(MachineState.State.NOT_ATTACHED));
	}

	synchronized public boolean isPaused() {
		return getMachineState().isPaused();
	}
	
	public void dispose() {
		if (machineThread != null) {
			machineThread.shutdown();
			try {
				machineThread.join(5000);
			} catch (Exception e) { e.printStackTrace(); }
		}
		if (driver != null) {
			driver.dispose();
		}
		if (getSimulatorDriver() != null) {
			getSimulatorDriver().dispose();
		}
		setState(new MachineState(MachineState.State.NOT_ATTACHED));
	}
	
	private Vector<MachineListener> listeners = new Vector<MachineListener>();
	
	public void addMachineStateListener(MachineListener listener) {
		listeners.add(listener);
		listener.machineStateChanged(new MachineStateChangeEvent(this,getMachineState()));
	}

	public void removeMachineStateListener(MachineListener listener) {
		listeners.remove(listener);
	}

	protected void emitStateChange(MachineState prev, MachineState current) {
		MachineStateChangeEvent e = new MachineStateChangeEvent(this, current, prev);
		Vector<MachineListener> lclone = (Vector<MachineListener>) listeners.clone();
		for (MachineListener l : lclone) {
			l.machineStateChanged(e);
		}
	}

	protected void emitProgress(MachineProgressEvent progress) {
		for (MachineListener l : listeners) {
			l.machineProgress(progress);
		}
	}

	protected void emitToolStatus(ToolModel tool) {
		MachineToolStatusEvent e = new MachineToolStatusEvent(this, tool);
		for (MachineListener l : listeners) {
			l.toolStatusChanged(e);
		}
	}
}
