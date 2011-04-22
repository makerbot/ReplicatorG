package replicatorg.machine;

import replicatorg.machine.model.ToolModel;

public class MachineToolStatusEvent {
	private Machine source;
	private ToolModel tool;
	
	public MachineToolStatusEvent(Machine source,
			ToolModel tool) {
		this.source = source;
		this.tool = tool;
	}
	
	public Machine getSource() { return source; }
	
	public ToolModel getTool() { return tool; }
	
}
