package replicatorg.machine;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;

/** Maintains a connection to one machine **/ 
	public class MachineLoader {
		
		private MachineInterface singletonMI;
		private String singletonMIType = "";
		
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
		
		/**
		 * 
		 * @return whatever the current singleton machine interface is.
		 */
		public MachineInterface getMachineInterface() {
			return singletonMI;
		}
		
		// Load a new machine
		public MachineInterface getMachineInterface(String machineType) {

			//if we have a matching singleton running, return it
			if(singletonMI != null && machineType.equals(singletonMIType) ) {
				return singletonMI;
			}
			//if we have nothing loaded, try to load that machine
			else if (singletonMI == null || machineType.equals("") ){
				singletonMI = MachineFactory.load(machineType, callbackHandler);
				if(singletonMI != null)
				{
					singletonMIType = machineType;
				}
				return singletonMI;
			}
			// if we don't have matching types, warn, then load a new singleton over this one
			else if (singletonMI != null && !machineType.equals(singletonMIType) ) {
					Base.logger.finest("MachineLoader loading new machine type " + machineType + " over existing machine " + singletonMIType);
					singletonMI.dispose();
					singletonMI = MachineFactory.load(machineType, callbackHandler);
					if(singletonMI != null)
					{
						singletonMIType = machineType;
					}
					return singletonMI;
			}
			return null;
		}
		
		
		/** True if the machine is loaded **/
		public boolean isLoaded() {
			return (singletonMI != null);
		}
		
		public boolean isConnected() {
			return (isLoaded() && singletonMI.isConnected());
		}
		
		@Deprecated
		public Driver getDriver() {
			if(!isLoaded()) {
				return null;
			}
			return singletonMI.getDriver();
		}
		
		
		// Do we ever want to do this?
		public void unload() {
			if (isLoaded()) {
				singletonMI.dispose();
				singletonMI = null;
			}
		}
		
		// Tell the machine to start a connection
		// TODO: This should be pushed into a separate class for managing connections.
		public void connect(String port) {
			if (!isLoaded()) {
				return;
			}
			
			singletonMI.connect(port);
		}
		
		// tell the machine to drop its connection
		public void disconnect() {
			if (isLoaded()) {
				singletonMI.disconnect();
			}
		}
		
		// Pass these on to our handler
		public void addMachineListener(MachineListener listener) {
			callbackHandler.addMachineListener(listener);
		}

		public void removeMachineListener(MachineListener listener) {
			callbackHandler.removeMachineListener(listener);
		}
		
		/// Clear out singleton object, in cases where we know we must, must, must rebuild the Machine objects
		public void clearSingleton() {
			if(singletonMI != null) {
				singletonMI.dispose();
				singletonMI = null;
			}

		}
	}