package replicatorg.drivers;

import java.util.EnumSet;

import replicatorg.machine.model.AxisId;

public interface OnboardParameters {
	EnumSet<AxisId> getInvertedParameters();
	void setInvertedParameters(EnumSet<AxisId> axes);
	
	String getMachineName();
	void setMachineName(String machineName);
	
	double getAxisHomeOffset(int axis);
	void setAxisHomeOffset(int axis, double d);
	
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
	
	
	boolean getCoolingFanEnabled();
	int getCoolingFanSetpoint();
	void setCoolingFanParameters(boolean enabled, int setpoint);

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
	
	PIDParameters getPIDParameters(int which);
	void setPIDParameters(int which, PIDParameters params);
	
	class ExtraFeatures {
		final static int CHA = 0;
		final static int CHB = 1;
		final static int CHC = 2;
		public boolean swapMotorController;
		public int heaterChannel;
		public int hbpChannel;
		public int abpChannel;
	}
	
	ExtraFeatures getExtraFeatures();
	void setExtraFeatures(ExtraFeatures features);
	
	public enum EstopType {
		NOT_PRESENT((byte)0x00),
		ACTIVE_HIGH((byte)0x01),
		ACTIVE_LOW((byte)0x02);
		
		final byte value;
		
		EstopType(byte value) {
			this.value = value;
		}
		
		public byte getValue() { return value; }
		
		public static EstopType estopTypeForValue(byte value) {
			if (value == ACTIVE_HIGH.value) { return ACTIVE_HIGH; }
			if (value == ACTIVE_LOW.value) { return ACTIVE_LOW; }
			return NOT_PRESENT;
		}
	}

	EstopType getEstopConfig();
	void setEstopConfig(EstopType estop);

	/** Reset the onboard parameters on the motherboard to factory settings. */ 
	void resetToFactory();

	/** Reset the onboard parameters on the extruder controller to factory settings. */ 
	void resetToolToFactory();

	
	public class CommunicationStatistics {
		public int packetCount;
		public int sentPacketCount;
		public int packetFailureCount;
		public int packetRetryCount;
		public int noiseByteCount;
	}
	
	CommunicationStatistics getCommunicationStatistics();
}
