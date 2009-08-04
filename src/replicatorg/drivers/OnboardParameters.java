package replicatorg.drivers;

import java.util.EnumSet;

import replicatorg.machine.model.Axis;

public interface OnboardParameters {
	EnumSet<Axis> getInvertedParameters();
	void setInvertedParameters(EnumSet<Axis> axes);
	
	String getMachineName();
	void setMachineName(String machineName);
	
	/**
	 * Returns whether onboard parameters are supported by the current machine.
	 * @return true if this version of the firmware supports OnboardParameters.
	 */
	boolean hasFeatureOnboardParameters();
}
