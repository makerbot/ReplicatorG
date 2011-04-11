package replicatorg.machine;

import java.util.logging.Level;

import replicatorg.app.Base;
import replicatorg.app.exceptions.SerialException;
import replicatorg.app.util.serial.Serial;
import replicatorg.drivers.Driver;
import replicatorg.drivers.UsesSerial;

/** Maintains a connection to one machine **/ 
	public class MachineLoader {
		private MachineControllerInterface machine;
		
		public MachineControllerInterface getMachine() {
			return machine;
		}
		
		/** True if the machine is loaded **/
		public boolean isLoaded() {
			return (machine != null);
		}
		
		public boolean isConnected() {
			return (isLoaded() && machine.getMachineState().getState() != MachineState.State.NOT_ATTACHED);
		}
		
		public Driver getDriver() {
			if(!isLoaded()) {
				return null;
			}
			return machine.getDriver();
		}
		
		// Load a new machine
		public boolean load(String machineType) {
			if (isLoaded()) {
				machine.dispose();
			}
			
			machine = MachineFactory.load(machineType);
			
			if (machine == null) {
				Base.logger.severe("Unable to connect to machine!");
				return false;
			}
			
			// TODO: tell the base class about this new machine???
			
			return true;
		}
		
		// Do we ever want to do this?
		public void unload() {
			if (isLoaded()) {
				machine.dispose();
				machine = null;
			}
		}
		
		// Tell the machine to start a connection
		// TODO: This should be pushed into a separate class for managing connections.
		public void connect() {
			if (!isLoaded()) {
				return;
			}
			
			// do connection!
			if (machine.getDriver() instanceof UsesSerial) {
				UsesSerial us = (UsesSerial)machine.getDriver();
				String targetPort;
				
				if (Base.preferences.getBoolean("serial.use_machines",true) &&
						us.isExplicit()) {
					targetPort = us.getPortName();
				} else {
					targetPort = Base.preferences.get("serial.last_selected", null);
				}
				
				if (targetPort == null) {
					Base.logger.severe("Couldn't find a port to use!");
					return;
				}
				
				try {
					synchronized(us) {
						Base.logger.severe("Connecting to port " + targetPort);
//						Serial current = us.getSerial();
//							Base.logger.fine("Current serial port: "+((current==null)?"null":current.getName())+", specified "+targetPort);
//							if (current == null || !current.getName().equals(targetPort)) {
							us.setSerial(new Serial(targetPort,us));
//							}
						machine.connect();
					}
				} catch (SerialException e) {
					String msg = e.getMessage();
					if (msg == null) { msg = "."; }
					else { msg = ": "+msg; }
					Base.logger.log(Level.WARNING,
							"Could not use specified serial port ("+targetPort+")"+ msg);
				}
			}
		}
		
		// tell the machine to drop its connection
		public void disconnect() {
			if (isLoaded()) {
				machine.disconnect();
			}
		}
	}