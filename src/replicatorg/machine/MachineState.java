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
	STOPPING,
	PAUSED,
	CAPTURING,
	PLAYBACK_BUILDING,
	PLAYBACK_PAUSED;
	
	public boolean isRunning() {
		return this == BUILDING || this == PAUSED ||
			this == PLAYBACK_BUILDING || this == PLAYBACK_PAUSED; 	
	}

	public boolean isPaused() {
		return  this == PAUSED || this == PLAYBACK_PAUSED; 	
	}
	
	public MachineState getPausedState() {
		if (this == BUILDING) return PAUSED;
		if (this == PLAYBACK_BUILDING) return PLAYBACK_PAUSED;
		return this;
	}

	public MachineState getUnpausedState() {
		if (this == PAUSED) return BUILDING;
		if (this == PLAYBACK_PAUSED) return PLAYBACK_BUILDING;
		return this;
	}
}
