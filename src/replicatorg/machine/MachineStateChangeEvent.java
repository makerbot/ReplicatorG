package replicatorg.machine;


public class MachineStateChangeEvent {
	private MachineInterface source;
	private MachineState previous = null;
	private MachineState current;
	
	public MachineStateChangeEvent(MachineInterface machine2, MachineState current) {
		this.source = machine2;
		this.current = current;
	}

	public MachineStateChangeEvent(MachineInterface source, MachineState current, 
			MachineState previous) {
		this.source = source;
		this.current = current;
		this.previous = previous;
	}
	
	public MachineInterface getSource() { return source; }
	
	public MachineState getState() { return current; }
	
	public MachineState getPreviousState() { return previous; }
}
