package replicatorg.machine;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;

/** Maintains a connection to one machine **/ 
	public class MachineLoader {
		private MachineInterface machine;
		
		MachineCallbackHandler callbackHandler;
		
		public MachineLoader() {
			callbackHandler = new MachineCallbackHandler();
			callbackHandler.start();
		}
		
		public void dispose() {
			if (callbackHandler != null) {
				callbackHandler.interrupt();

				// Wait 5 seconds for the thread to stop.
				try {
					callbackHandler.join(5000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		public MachineInterface getMachine() {
			return machine;
		}
		
		/** True if the machine is loaded **/
		public boolean isLoaded() {
			return (machine != null);
		}
		
		public boolean isConnected() {
			return (isLoaded() && machine.isConnected());
		}
		
		@Deprecated
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
				machine = null;
			}
			
			machine = MachineFactory.load(machineType, callbackHandler);
			
			if (machine == null) {
				// no err,  the above load() function prints an error
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
		
		// Pass these on to our handler
		public void addMachineListener(MachineListener listener) {
			callbackHandler.addMachineListener(listener);
		}

		public void removeMachineListener(MachineListener listener) {
			callbackHandler.removeMachineListener(listener);
		}
	}