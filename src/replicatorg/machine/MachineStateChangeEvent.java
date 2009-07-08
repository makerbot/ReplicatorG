package replicatorg.machine;

import replicatorg.app.MachineController;

public class MachineStateChangeEvent {
	private MachineController source;
	private MachineState previous = null;
	private MachineState current;
	
	public MachineStateChangeEvent(MachineController source, MachineState current) {
		this.source = source;
		this.current = current;
	}

	public MachineStateChangeEvent(MachineController source, MachineState current, 
			MachineState previous) {
		this.source = source;
		this.current = current;
		this.previous = previous;
	}
	
	public MachineController getSource() { return source; }
	
	public MachineState getState() { return current; }
	
	public MachineState getPreviousState() { return previous; }
}
