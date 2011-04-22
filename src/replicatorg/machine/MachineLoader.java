package replicatorg.machine;

import replicatorg.app.Base;
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
			return (isLoaded() && machine.isConnected());
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
		public void connect(String port) {
			if (!isLoaded()) {
				return;
			}
			
			machine.connect(port);
		}
		
		// tell the machine to drop its connection
		public void disconnect() {
			if (isLoaded()) {
				machine.disconnect();
			}
		}
	}