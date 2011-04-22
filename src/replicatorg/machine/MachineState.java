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
		/** There is no connection to the machine, and no attempt is in progress. **/ 
		NOT_ATTACHED,
		/** An attempt to contact the machine is in progress. **/
		CONNECTING,
		/** The controller has successfully contacted the machine, and is ready
		 * for input. **/
		READY,
		/** The machine is building from a driver-provide source **/
		BUILDING,
		/** The machine is building, but is currently paused. **/
		PAUSED,
		/** The machine reported a fatal error, and is halted. **/
		ERROR,
	};
	
	private State state;

	/** Create a machine state with the given state characteristic and no target. */
	public MachineState(State state) {
		this.state = state;
	}
	
	public State getState() { return state; }

	
	public boolean isReady() {
		return state == State.READY;
	}
	
	/** True if the machine is actively building */
	public boolean isBuilding() {
		return state == State.BUILDING || state == State.PAUSED; 
	}
	
	// TODO: Error state could possibly be considered connected???
	public boolean isConnected() {
		return state == State.READY
			|| state == State.BUILDING
			|| state == State.PAUSED;
	}
	
	public boolean isPaused() {
		return (state == State.PAUSED);
	}
	
	public MachineState clone() {
		try {
			return (MachineState)super.clone();
		} catch (CloneNotSupportedException e) {
			// Clone is supported.
			throw new RuntimeException(e);
		}
	}
	
	public boolean equals(MachineState other) {
		return other.state == state;
	}
}
