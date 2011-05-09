package replicatorg.machine;

import replicatorg.drivers.commands.DriverCommand;
import replicatorg.machine.Machine.RequestType;
import replicatorg.model.GCodeSource;

public class MachineCommand {

	final RequestType type;
	final GCodeSource source;
	final String remoteName;
	final DriverCommand command;

	public MachineCommand(RequestType type, GCodeSource source,
			String remoteName) {
		this.type = type;
		this.source = source;
		this.remoteName = remoteName;
		
		this.command = null;
	}

	public MachineCommand(RequestType type, DriverCommand command) {
		this.type = type;
		this.command = command;
		
		this.source = null;
		this.remoteName = null;
	}
}