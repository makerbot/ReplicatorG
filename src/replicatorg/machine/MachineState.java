/**
 * 
 */
package replicatorg.machine;

/**
 * The MachineState indicates the current high-level status of the machine.
 * 
 * It's become clear that pause status should be taken out of the state and put
 * in its own modifier in the machine model.  Not today, though.
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
	UPLOADING,
	SIMULATING,
	SIMULATING_PAUSED,
	PLAYBACK_BUILDING,
	PLAYBACK_PAUSED;
	
	public boolean isRunning() {
		return isPaused() ||
			this == BUILDING || 
			this == UPLOADING ||
			this == SIMULATING || 
			this == PLAYBACK_BUILDING; 	
	}

	public boolean isPaused() {
		return  this == PAUSED || this == PLAYBACK_PAUSED ||
			this == SIMULATING_PAUSED;
	}
	
	public boolean isSimulating() {
		return this == SIMULATING || this == SIMULATING_PAUSED;
	}
	
	public MachineState getPausedState() {
		if (this == BUILDING) return PAUSED;
		if (this == PLAYBACK_BUILDING) return PLAYBACK_PAUSED;
		if (this == SIMULATING) return SIMULATING_PAUSED;
		return this;
	}

	public MachineState getUnpausedState() {
		if (this == PAUSED) return BUILDING;
		if (this == PLAYBACK_PAUSED) return PLAYBACK_BUILDING;
		if (this == SIMULATING_PAUSED) return SIMULATING;
		return this;
	}
}
