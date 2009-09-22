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
	public enum State {
		NOT_ATTACHED,
		CONNECTING,
		AUTO_SCAN,
		READY,
		ESTIMATING,
		BUILDING,
		MANUAL_CONTROL,
		STOPPING,
		PLAYBACK
	};
	
	public enum Target {
		NONE,
		MACHINE,
		SIMULATOR,
		SD_UPLOAD,
		FILE
	};
	
	private State state = State.NOT_ATTACHED;
	private Target target = Target.NONE;
	private boolean paused = false;
	
	public MachineState() {
	}
	
	public MachineState(State state) {
		this.state = state;
	}
	
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
	
	public boolean isBuilding() {
		return state == State.BUILDING ||
			state == State.PLAYBACK; 
	}

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
	 * By default, this always resets the pause status to false.
	 * @param state
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
	
	public MachineState clone() {
		try {
			return (MachineState)super.clone();
		} catch (CloneNotSupportedException e) {
			// Clone is supported.
			throw new RuntimeException(e);
		}
	}
}
