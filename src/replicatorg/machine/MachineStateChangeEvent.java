package replicatorg.machine;


public class MachineStateChangeEvent {
	private final MachineInterface source;
	private final MachineState current;
	private final String message;
	
	public MachineStateChangeEvent(MachineInterface machine2, MachineState current) {
		this.source = machine2;
		this.current = current;
		this.message = null;
	}

	public MachineStateChangeEvent(MachineInterface source, MachineState current, 
			String message) {
		this.source = source;
		this.current = current;
		this.message = message;
	}
	
	public MachineInterface getSource() { return source; }
	
	public MachineState getState() { return current; }
		
	public String getMessage() { return message; }
}
