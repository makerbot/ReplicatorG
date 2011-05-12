package replicatorg.machine;

import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;
import replicatorg.app.tools.XML;
import replicatorg.drivers.Driver;
import replicatorg.drivers.DriverError;
import replicatorg.drivers.DriverFactory;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SDCardCapture;
import replicatorg.drivers.SimulationDriver;
import replicatorg.drivers.StopException;
import replicatorg.drivers.UsesSerial;
import replicatorg.drivers.commands.AssessState;
import replicatorg.drivers.commands.DriverCommand;
import replicatorg.machine.Machine.JobTarget;
import replicatorg.machine.Machine.RequestType;
import replicatorg.machine.builder.MachineBuilder;
import replicatorg.machine.builder.Direct;
import replicatorg.machine.builder.ToLocalFile;
import replicatorg.machine.builder.ToRemoteFile;
import replicatorg.machine.builder.UsingRemoteFile;
import replicatorg.machine.model.MachineModel;
import replicatorg.model.GCodeSource;
import replicatorg.model.GCodeSourceCollection;
import replicatorg.model.StringListSource;

/**
 * The MachineThread is responsible for communicating with the machine.
 */
class MachineThread extends Thread {

	AssessStatusThread statusThread;
	
	// TODO: Should this be here?
	class AssessStatusThread extends Thread {
		
		MachineThread machineThread;
		
		public AssessStatusThread(MachineThread machineThread) {
			super("Assess Status");
			this.machineThread = machineThread;
		}
		
		public void run() {
			while (true) {
				try {
					synchronized(this) {
						// Send out a request, then sleep for a bit, then start over.
						DriverCommand assessCommand = new AssessState();
						
						machineThread.scheduleRequest(new MachineCommand(
								RequestType.RUN_COMMAND, assessCommand));
						
						sleep(1000);
					}
				} catch(InterruptedException e) {
					Base.logger.fine("taking assess status thread down");
					break;
				}
			}
		}
	}
	
	// TODO: Rethink this.
	class Timer {
		private long lastEventTime = 0;
		private boolean enabled = false;
		private long intervalMs = 1000;
		
		public void start(long interval) {
			enabled = true;
			intervalMs = interval;
		}
		
		public void stop() {
			enabled = false;
		}
		
		// send out updates
		public boolean elapsed() {
			if (!enabled) {
				return false;
			}
			long curMillis = System.currentTimeMillis();
			if (lastEventTime + intervalMs <= curMillis) {
				lastEventTime = curMillis;
				
				return true;
			}
			return false;
		}
	}
	
	private Timer pollingTimer;

	// Link of machine commands to run
	ConcurrentLinkedQueue<MachineCommand> pendingQueue;
		
	// this is the xml config for this machine.
	private Node machineNode;
	
	private Machine controller;
	
	// our warmup/cooldown commands
	private Vector<String> warmupCommands;
	private Vector<String> cooldownCommands;
	
	// The name of our machine.
	private String name;
	
	// Things that belong to a job
		// estimated build time in millis
		private double estimatedBuildTime = 0;
	
		// Build statistics
		private double startTimeMillis = -1;
	
	// Our driver object. Null when no driver is selected.
	private Driver driver = null;
	
	// the simulator driver
	private SimulationDriver simulator;
	
	private MachineState state = new MachineState(MachineState.State.NOT_ATTACHED);

	// ???
	MachineModel cachedModel = null;
	
	private MachineBuilder machineBuilder;
	
	public MachineThread(Machine controller, Node machineNode) {
		super("Machine Thread");
		
		pollingTimer = new Timer();
		
		pendingQueue = new ConcurrentLinkedQueue<MachineCommand>();
		
		// save our XML
		this.machineNode = machineNode;
		this.controller = controller;
		
		// load our various objects
		loadDriver();
		loadExtraPrefs();
		parseName();
		
		statusThread = new AssessStatusThread(this);
		statusThread.start();
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

	GCodeSource buildGCodeJob(GCodeSource source) {
		Vector<GCodeSource> sources = new Vector<GCodeSource>();
		sources.add(new StringListSource(warmupCommands));
		sources.add(source);
		sources.add(new StringListSource(cooldownCommands));
		return new GCodeSourceCollection(sources);
	}
	
	private String readyMessage() {
		return "Machine " + getMachineName() + " ready";
	}
	
	private String notConnectedMessage() {
		return "Not Connected";
	}
	
	private String buildingMessage() {
		return "Building...";
	}
	
	// Respond to a command from the machine controller
	void runCommand(MachineCommand command) {		
		switch(command.type) {
		case CONNECT:
			if (state.getState() == MachineState.State.NOT_ATTACHED) {
				
				// TODO: This message doesn't take an explicit override into account.
				setState(new MachineState(MachineState.State.CONNECTING),
						"Connecting to " + getMachineName() + " on " + command.remoteName);
				
				boolean connected = false;
				
				// First, try to open the serial port
				if (driver instanceof UsesSerial) {
					UsesSerial us = (UsesSerial)driver;
					
					String targetPort = command.remoteName;
					
					if (us.isExplicit()) {
						targetPort = us.getPortName();
					}
					
					us.openSerial(targetPort);
					connected = us.isConnected();
				}
				
				if (!connected) {
					// If we couldn't connect, move back to being detached.
					String errorMessage = null;
					
					if (driver.hasError()) {
						errorMessage = driver.getError().getMessage();
					}
					
					setState(new MachineState(MachineState.State.NOT_ATTACHED), errorMessage);
				}
				else {
					
					// If the port is open, try to talk to the machine over it.
					driver.initialize();
					if (driver.isInitialized()) {
						readName();
						setState(new MachineState(MachineState.State.READY),
								readyMessage());
					} else {
						setState(new MachineState(MachineState.State.NOT_ATTACHED));
					}
				}
			}
			break;
		case DISCONNECT:
			// TODO: This seems wrong
			if (state.isConnected()) {
				driver.uninitialize();
				setState(new MachineState(MachineState.State.NOT_ATTACHED), notConnectedMessage());
			
				if (driver instanceof UsesSerial) {
					UsesSerial us = (UsesSerial)driver;
					us.closeSerial();
				}
			}
			break;
		case RESET:
			if (state.isConnected()) {
				driver.reset();
				readName();
				setState(new MachineState(MachineState.State.READY),
						readyMessage());
			}
			break;
		case BUILD_DIRECT:
			if (state.canPrint()) {
				startTimeMillis = System.currentTimeMillis();
				
				pollingTimer.start(1000);

				if (!isSimulating()) {
					driver.getCurrentPosition(false); // reconcile position
				}
				
				// Pad the job with start and end code
				GCodeSource combinedSource = buildGCodeJob(command.source);
				
				machineBuilder = new Direct(driver, simulator, combinedSource);
				
				// TODO: This shouldn't be done here?
				driver.invalidatePosition();
				
				// TODO: Where should the build name be specified?
				setState(new MachineState(MachineState.State.BUILDING), buildingMessage());
			}
			break;
		case SIMULATE:
//			// TODO: Implement this.
//			setState(new MachineState(MachineState.State.BUILDING));
			break;
		case BUILD_TO_REMOTE_FILE:
			if (state.canPrint()) {
				if (!(driver instanceof SDCardCapture)) {
					break;
				}
				
				startTimeMillis = System.currentTimeMillis();

				pollingTimer.start(1000);
				
				// Pad the job with start and end code
				GCodeSource combinedSource = buildGCodeJob(command.source);
				
				machineBuilder = new ToRemoteFile(driver, simulator,
											combinedSource, command.remoteName);
	
				// TODO: This shouldn't be done here?
				driver.invalidatePosition();
				setState(new MachineState(MachineState.State.BUILDING));
			}
			break;
		case BUILD_TO_FILE:
			/** We will accept a disconnected machine or a ready machine. */
			if (state.canPrint() || state.getState() == MachineState.State.NOT_ATTACHED) {
				if (!(driver instanceof SDCardCapture)) {
					break;
				}
				
				startTimeMillis = System.currentTimeMillis();

				// There is no need to reconcile the position.
				pollingTimer.start(1000);
				
				// Pad the job with start and end code
				GCodeSource combinedSource = buildGCodeJob(command.source);
				
				machineBuilder = new ToLocalFile(driver, simulator,
											combinedSource, command.remoteName);
				if (state.canPrint()) {
					setState(new MachineState(MachineState.State.BUILDING), buildingMessage());
				} else {
					setState(new MachineState(MachineState.State.BUILDING_OFFLINE), buildingMessage());
				}
			}
			break;
		case BUILD_REMOTE:
			if (state.canPrint()) {
				if (!(driver instanceof SDCardCapture)) {
					break;
				}
				
				startTimeMillis = System.currentTimeMillis();
				
				pollingTimer.start(1000);
				
				machineBuilder = new UsingRemoteFile((SDCardCapture)driver, command.remoteName);
			
				// TODO: what about this?
				driver.getCurrentPosition(false); // reconcile position
				setState(new MachineState(MachineState.State.BUILDING), buildingMessage());
			}
			break;
		case PAUSE:
			if (state.getState() == MachineState.State.BUILDING) {
				setState(new MachineState(MachineState.State.PAUSED), "Build paused");
			}
			break;
		case UNPAUSE:
			if (state.getState() == MachineState.State.PAUSED) {
				setState(new MachineState(MachineState.State.BUILDING), buildingMessage());
			}
			break;
		case STOP_MOTION:
			driver.stop(false);
			
			if (state.getState() == MachineState.State.BUILDING) {
				setState(new MachineState(MachineState.State.READY),
						readyMessage());
			}
			else if (state.getState() == MachineState.State.BUILDING_OFFLINE) {
				setState(new MachineState(MachineState.State.NOT_ATTACHED),
						notConnectedMessage());
			}
			break;
		case STOP_ALL:
			// TODO: This should be handled at the driver level?
			driver.getMachine().currentTool().setTargetTemperature(0);
			driver.getMachine().currentTool().setPlatformTargetTemperature(0);
			
			driver.stop(true);
			
			if (state.getState() == MachineState.State.BUILDING) {
				setState(new MachineState(MachineState.State.READY),
						readyMessage());
			}
			else if (state.getState() == MachineState.State.BUILDING_OFFLINE) {
				setState(new MachineState(MachineState.State.NOT_ATTACHED),
						notConnectedMessage());
			}
			break;			
//		case DISCONNECT_REMOTE_BUILD:
//			// TODO: This is wrong.
//			
//			if (state.getState() == MachineState.State.BUILDING_REMOTE) {
//				return; // send no further packets to machine; let it go on its own
//			}
//			
//			if (state.isBuilding()) {
//				setState(MachineState.State.STOPPING);
//			}
//			break;
		case RUN_COMMAND:
			if (state.isConnected()) {
				boolean completed = false;
				// TODO: Don't get stuck in a loop here!
				
				while(!completed) {
					try {
						command.command.run(driver);
						completed = true;
					} catch (RetryException e) {
					} catch (StopException e) {
					}
				}
			}
			break;
		case SHUTDOWN:
			//TODO: Dispose of everything here.
			setState(new MachineState(MachineState.State.NOT_ATTACHED), notConnectedMessage());
			interrupt();
			break;
		default:
			Base.logger.severe("Ignored command: " + command.type.toString());
		}
	}
	
	/**
	 * Main machine thread loop.
	 */
	public void run() {
		// This is our main loop.
		while (true) {
			
			// First, check if the driver registered any errors
			if (driver.hasError()) {
				DriverError error = driver.getError();

				if(state.isConnected() && error.getDisconnected()) {
					// If we were connected, but this error causes us to disconnect,
					// transition to a disconnected state
					setState(new MachineState(MachineState.State.NOT_ATTACHED),
							error.getMessage());
				}
				else {
					// Otherwise, transition to an error state, where we can still
					// configure the machine, but can't print.
					setState(new MachineState(MachineState.State.ERROR),
							error.getMessage());
				}
			}
			
			//
			
			// Check for and run any control requests that might be in the queue.
			while (!pendingQueue.isEmpty()) {
				runCommand(pendingQueue.remove());
			}
			
			// If we are building
			if ( state.isBuilding() && !state.isPaused() ) {
				//run another instruction on the machine.
				machineBuilder.runNext();
				
				// Check the status poll machine.
				if (pollingTimer.elapsed()) {
					if (Base.preferences.getBoolean("build.monitor_temp",false)) {
						driver.readTemperature();
						controller.emitToolStatus(driver.getMachine().currentTool());
					}
				}
				
				// Send out a progress event
				// TODO: Should these be rate limited?
				MachineProgressEvent progress = 
					new MachineProgressEvent((double)System.currentTimeMillis()-startTimeMillis,
							estimatedBuildTime,
							machineBuilder.getLinesProcessed(),
							machineBuilder.getLinesTotal());
				controller.emitProgress(progress);
				
				if (machineBuilder.finished()) {
					// TODO: Exit correctly.
					if (state.getState() == MachineState.State.BUILDING) {
						setState(new MachineState(MachineState.State.READY),
								readyMessage());
					} else {
						setState(new MachineState(MachineState.State.NOT_ATTACHED),
								notConnectedMessage());
					}
					
					pollingTimer.stop();
				}
			}
			
			// If there is nothing to do, sleep.
			if ( !state.isBuilding() ) {
				try {
					synchronized(this) {
						wait();
					}
				} catch(InterruptedException e) {
					break;
				}
			}
			
			// If we get interrupted, break out of the main loop.
			if (Thread.interrupted()) {
		        break;
			}
		}
		
		Base.logger.fine("MachineThread interrupted, terminating.");
		dispose();
	}
	
	public boolean scheduleRequest(MachineCommand request) {
		pendingQueue.add(request);
		synchronized(this) { notify(); }
		
		return true;
	}
	
	public boolean isReadyToPrint() { return state.canPrint(); }
	

	/** True if the machine's build is going to the simulator. */
	public boolean isSimulating() {
		// TODO: implement this.
		return false;
	}
	
	// TODO: Put this somewhere else
	public boolean isInteractiveTarget() {
		if (machineBuilder != null) {
			return machineBuilder.isInteractive();
		}
		return false;
	}
	
	public JobTarget getTarget() {
		if (machineBuilder != null) {
			return machineBuilder.getTarget();
		}
		return JobTarget.NONE;
	}
	
	public int getLinesProcessed() {
		if (machineBuilder != null) {
			return machineBuilder.getLinesProcessed();
		}
		return -1;
	}
	
	public MachineState getMachineState() {
		return state.clone();
	}
	
	
	/**
	 * Set the a machine state.  If the state is not the current state, a state change
	 * event will be emitted and the machine thread will be notified.  
	 * @param state the new state of the machine.
	 */
	private void setState(MachineState state) {
		setState(state, null);
	}
	
	private void setState(MachineState state, String message) {
		MachineState oldState = this.state;
		this.state = state;
		if (!oldState.equals(state)) {
			controller.emitStateChange(state, message);
		}
	}
	
	public Driver getDriver() {
		return driver;
	}
	
	public SimulationDriver getSimulator() {
		return simulator;
	}
	
	public boolean isConnected() {
		return (driver != null && driver.isInitialized());
	}
	
	public void loadDriver() {
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
	
	private void dispose() {
		if (driver != null) {
			driver.dispose();
		}
		if (simulator != null) {
			simulator.dispose();
		}
		
		if (statusThread != null) {
			try {
				statusThread.interrupt();
				statusThread.join(1000);
			} catch (InterruptedException e) {
			}
		}
		
		setState(new MachineState(MachineState.State.NOT_ATTACHED));
	}
	
	public void readName() {
		if (driver instanceof OnboardParameters) {
			String n = ((OnboardParameters)driver).getMachineName();
			if (n != null && n.length() > 0) {
				name = n;
			}
			else {
				parseName(); // Use name from XML file instead of reusing name from last connected machine
			}
		}
	}
	
	private void parseName() {
		NodeList kids = machineNode.getChildNodes();

		for (int j = 0; j < kids.getLength(); j++) {
			Node kid = kids.item(j);

			if (kid.getNodeName().equals("name")) {
				name = kid.getFirstChild().getNodeValue().trim();
				return;
			}
		}

		name = "Unknown";
	}
	
	private MachineModel loadModel() {
		MachineModel model = new MachineModel();
		model.loadXML(machineNode);
		return model;
	}
	
	public MachineModel getModel() {
		if (cachedModel == null) { cachedModel = loadModel(); }
		return cachedModel;
	}
	
	// TODO: Make this a command.
	public void setEstimatedBuildTime(double estimatedBuildTime) {
		this.estimatedBuildTime = estimatedBuildTime;
	}
	
	public String getMachineName() { return name; }
}