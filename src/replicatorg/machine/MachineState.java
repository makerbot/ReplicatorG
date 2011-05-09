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
		/** There is no physically connected machine, but an offline build is in process. **/
		BUILDING_OFFLINE,
		/** The machine is building, but is currently paused. **/
		PAUSED,
		/** The machine reported a fatal error, and is halted (but still connected). **/
		ERROR,
	};
	
	private State state;

	/** Create a machine state with the given state characteristic and no target. */
	public MachineState(State state) {
		this.state = state;
	}
	
	public State getState() { return state; }
	
	/** True if the machine is actively building */
	public boolean isBuilding() {
		return state == State.BUILDING || state == State.PAUSED || state == State.BUILDING_OFFLINE; 
	}
	
	// TODO: Error state could possibly be considered connected???
	public boolean isConnected() {
		return state == State.READY
			|| state == State.BUILDING
			|| state == State.PAUSED
			|| state == State.ERROR;
	}
	
	/**
	 * @return True if the machine is ready to start printing
	 */
	public boolean canPrint() {
		return state == State.READY;
	}
		
	public boolean isPrinting() {
		return state == State.BUILDING
			|| state == State.BUILDING_OFFLINE
			|| state == State.PAUSED;
	}
	
	public boolean isPaused() {
		return (state == State.PAUSED);
	}
	
	public boolean isConfigurable() {
		return (isConnected() && !isPrinting());
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
