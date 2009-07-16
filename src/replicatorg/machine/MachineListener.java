package replicatorg.machine;

public interface MachineListener {
	public void machineStateChanged(MachineStateChangeEvent evt);
	
	public void machineProgress(MachineProgressEvent event);
}
