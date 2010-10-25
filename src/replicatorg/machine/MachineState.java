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
public class MachineState extends Object implements Cloneable {
	/**
	 * The state of a machine controller abstraction.
	 */
	public enum State {
		/** There is no connection to the machine, and no attempt is in progress. */ 
		NOT_ATTACHED,
		/** An attempt to contact the machine is in progress. */
		CONNECTING,
		/** The controller has successfully contacted the machine, and is ready
		 * for input. */
		READY,
		/** A build is in progress. */
		BUILDING,
		/** The machine is stopping operation. */
		STOPPING,
		/** The machine is building from an SD card. */
		PLAYBACK,
		/** The machine is being reset */
		RESET
	};
	
	/**
	 * Indicate the target of the current machine operations.
	 */
	public enum Target {
		/** No target selected. */
		NONE,
		/** Operations are performed on a physical machine. */
		MACHINE,
		/** Operations are being simulated. */
		SIMULATOR,
		/** Operations are being captured to an SD card on the machine. */
		SD_UPLOAD,
		/** Operations are being captured to a file. */
		FILE
	};
	
	private State state = State.NOT_ATTACHED;
	private Target target = Target.NONE;
	/** True if the machine is paused. */
	private boolean paused = false;
	
	/** Create an unattached machine state with no target. */
	public MachineState() {
	}
	
	/** Create a machine state with the given state characteristic and no target. */
	public MachineState(State state) {
		this.state = state;
	}
	
	/** Create a machine state with the given state and target. */
	public MachineState(State state, Target target) {
		this.state = state;
		this.target = target;
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	public boolean isReady() {
		return state == State.READY;
	}
	
	/** True if the machine is actively building, either over the connection or
	 * from a file on the SD card. */
	public boolean isBuilding() {
		return state == State.BUILDING ||
			state == State.PLAYBACK; 
	}

	/** True if the machine's build is going to the simulator. */
	public boolean isSimulating() {
		return state == State.BUILDING && target == Target.SIMULATOR;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	
	public void setTarget(Target target) {
		this.target = target;
	}

	/**
	 * Set the new machine state.
	 * This call resets the pause status to false.
	 */
	public void setState(State state) {
		this.state = state;
		this.paused = false;
	}
	
	public Target getTarget() { return target; }
	
	public State getState() { return state; }
	
	public boolean isInteractiveTarget() {
		return this.target == Target.MACHINE ||
			this.target == Target.SIMULATOR; 	
	}
	
	public boolean isConnected() {
		return state != State.NOT_ATTACHED &&
			state != State.CONNECTING;
	}
	
	public MachineState clone() {
		try {
			return (MachineState)super.clone();
		} catch (CloneNotSupportedException e) {
			// Clone is supported.
			throw new RuntimeException(e);
		}
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof MachineState)) return false;
		MachineState other = (MachineState)o;
		return other.state == state &&
			other.target == target &&
			other.paused == paused;
	}
}
