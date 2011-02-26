package replicatorg.machine;

import replicatorg.machine.model.ToolModel;

public class MachineToolStatusEvent {
	private MachineController source;
	private ToolModel tool;
	
	public MachineToolStatusEvent(MachineController source,
			ToolModel tool) {
		this.source = source;
		this.tool = tool;
	}
	
	public MachineController getSource() { return source; }
	
	public ToolModel getTool() { return tool; }
	
}
