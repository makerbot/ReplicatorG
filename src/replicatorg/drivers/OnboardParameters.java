package replicatorg.drivers;

import java.util.EnumSet;

import replicatorg.machine.model.Axis;

public interface OnboardParameters {
	EnumSet<Axis> getInvertedParameters();
	void setInvertedParameters(EnumSet<Axis> axes);
	
	String getMachineName();
	void setMachineName(String machineName);
	
	public enum EndstopType {
		NOT_PRESENT((byte)0x00),
		INVERTED((byte)0x9F),
		NON_INVERTED((byte)0x80);
		
		final byte value;
		
		EndstopType(byte value) {
			this.value = value;
		}
		
		public byte getValue() { return value; }
		
		public static EndstopType endstopTypeForValue(byte value) {
			if ((value & 1<<7) == 0) { return NOT_PRESENT; }
			return ((value & 1) == 0)?NON_INVERTED:INVERTED;
		}
	}
	
	EndstopType getInvertedEndstops();
	void setInvertedEndstops(EndstopType endstops);
	
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
	
	class PIDParameters {
		public float p;
		public float i;
		public float d;
	}
	
	PIDParameters getPIDParameters();
	void setPIDParameters(PIDParameters params);
	
	/** Reset the onboard parameters on the motherboard to factory settings. */ 
	void resetToFactory();

	/** Reset the onboard parameters on the extruder controller to factory settings. */ 
	void resetToolToFactory();

}
