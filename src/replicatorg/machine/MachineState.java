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
		/** The machine is building from a driver-provide source */
		BUILDING,
		/** The machine is building from it's internal memory. */
		BUILDING_REMOTE,
		/** The machine is stopping operation. */
		STOPPING,
		/** The machine is being reset */
		RESET
	};
	
	private State state = State.NOT_ATTACHED;
	
	/** True if the machine is paused. */
	private boolean paused;
	
	/** Create an unattached machine state with no target. */
	public MachineState() {
		state = State.NOT_ATTACHED;
		paused = false;
	}
	
	/** Create a machine state with the given state characteristic and no target. */
	public MachineState(State state) {
		this.state = state;
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
			state == State.BUILDING_REMOTE; 
	}



	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	
	/**
	 * Set the new machine state.
	 * This call resets the pause status to false.
	 */
	public void setState(State state) {
		this.state = state;
		this.paused = false;
	}
	
	public State getState() { return state; }
	
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
			other.paused == paused;
	}
}
