package replicatorg.drivers;

import java.util.EnumSet;

import replicatorg.machine.model.Axis;

public interface OnboardParameters {
	EnumSet<Axis> getInvertedParameters();
	void setInvertedParameters(EnumSet<Axis> axes);
	
	String getMachineName();
	void setMachineName(String machineName);
}
