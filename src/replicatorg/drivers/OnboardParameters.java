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
		
	void createThermistorTable(int which, double r0, double t0, double beta);
	int getR0(int which);
	int getT0(int which);
	int getBeta(int which);
	

	class BackoffParameters {
		public int stopMs;
		public int reverseMs;
		public int forwardMs;
		public int triggerMs;
	}
	
	BackoffParameters getBackoffParameters();
	void setBackoffParameters(BackoffParameters params);
	
	/** Reset the onboard parameters to the factory settings. */ 
	void resetToFactory();
}
