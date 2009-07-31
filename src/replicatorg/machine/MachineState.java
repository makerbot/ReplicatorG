/**
 * 
 */
package replicatorg.machine;

/**
 * The MachineState indicates the current high-level status of the machine.
 * 
 * @author phooky
 * 
 */
public enum MachineState {
	NOT_ATTACHED,
	CONNECTING,
	AUTO_SCAN,
	READY,
	ESTIMATING,
	BUILDING,
	PAUSED,
	STOPPING;
	
	public boolean isRunning() {
		return this == BUILDING || this == PAUSED;
	}
}
