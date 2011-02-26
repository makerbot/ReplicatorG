package replicatorg.machine;


public class MachineStateChangeEvent {
	private MachineControllerInterface source;
	private MachineState previous = null;
	private MachineState current;
	
	public MachineStateChangeEvent(MachineControllerInterface machine2, MachineState current) {
		this.source = machine2;
		this.current = current;
	}

	public MachineStateChangeEvent(MachineController source, MachineState current, 
			MachineState previous) {
		this.source = source;
		this.current = current;
		this.previous = previous;
	}
	
	public MachineControllerInterface getSource() { return source; }
	
	public MachineState getState() { return current; }
	
	public MachineState getPreviousState() { return previous; }
}
