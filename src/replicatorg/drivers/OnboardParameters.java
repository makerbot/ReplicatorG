package replicatorg.drivers;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolModel;


public interface OnboardParameters {
	
	/// Some tools store 2 heating data tables, these flags are used to indicate which table we
	// wish to access, the extruder heater, or the build platform heater.
	public static final int EXTRUDER = 0;
	public static final int BUILD_PLATFORM = 1;
	
	//// Get a list of all toolheads for which we save onboard preferences
	List<Integer> toolheadsWithStoredData();
	
	///Return a list of Axes that are flagged as inverted in the firmware
	EnumSet<AxisId> getInvertedAxes();

	/// Returns a set of Axes that are overridden or hijacked, 
	/// and a string to indicate what they are overridden or hijacked for.
	EnumMap<AxisId,String> getAxisAlises();
	
	void setInvertedAxes(EnumSet<AxisId> axes);
	
	String getMachineName();
	void setMachineName(String machineName);
	
	/// Returns true if this machine can verify the connected hardware
	/// is valid 
	public boolean canVerifyMachine(); 
	
	/// Returns true if the connected machine is verified to be the 
	/// proper type
	public boolean verifyMachineId();

	
	double getAxisHomeOffset(int axis);
	void setAxisHomeOffset(int axis, double d);
        
        /// returns true if the target machine stores toolhead offsets
	boolean hasAcceleration();
        /// Store acceleration settings to eeprom 
        void setAccelerationStatus(byte status);
        void setAccelerationRate(int rate);
        void setAxisAccelerationRate(int axis, int rate);
        void setAxisJerk(int axis, double jerk);
        void setAccelerationMinimumSpeed(int speed);
        /// Read acceleration settings from eeprom
        boolean getAccelerationStatus();
        int getAccelerationRate();
        int getAxisAccelerationRate(int axis);
        double getAxisJerk(int axis);
        int getAccelerationMinimumSpeed();
	
        
	
	/// returns true if the target machine stores toolhead offsets
	boolean hasToolheadsOffset();

	/// return the total toolhead offset (tolerance error, plus
	/// standard toolhead distance) in mm
	double getToolheadsOffset(int axis);

        /// set to EEPROM the distance out of tolerance the 
        /// specified axis is
        void eepromStoreToolDelta(int axis, double distanceMm);
	
	
	public enum EndstopType {
		
		NOT_PRESENT((byte)0x00), //no endstops present 
		ALL_INVERTED((byte)0x9F),// 5 ends stops (bits 0:4) plus has_endstops flag (bit 7)
		NON_INVERTED((byte)0x80); // only has_endstops flag (bit 7)
		
		final byte value; //byte flag for endstop inversion status
		
		EndstopType(byte value) {
			this.value = value;
		}
		
		public byte getValue() { return value; }
		
		public static EndstopType endstopTypeForValue(byte value) {
			if ((value & 1<<7) == 0) 
			{ return NOT_PRESENT; }
			return ((value & 1) == 0)?NON_INVERTED:ALL_INVERTED;
		}
	}
	
	
	EndstopType getInvertedEndstops();
	void setInvertedEndstops(EndstopType endstops);
	
	/**
	 * Returns whether onboard parameters are supported by the current machine.
	 * @return true if this version of the firmware supports OnboardParameters.
	 */
	boolean hasFeatureOnboardParameters();
		
	
	void createThermistorTable(int which, double r0, double t0, double beta, int toolIndex);
	int getR0(int which, int toolIndex);
	int getT0(int which, int toolIndex);
	int getBeta(int which, int toolIndex);
	
	
	boolean getCoolingFanEnabled(int toolIndex);
	int getCoolingFanSetpoint(int toolIndex);
	void setCoolingFanParameters(boolean enabled, int setpoint, int toolIndex);

	class BackoffParameters {
		public int stopMs;
		public int reverseMs;
		public int forwardMs;
		public int triggerMs;
	}
	
	BackoffParameters getBackoffParameters(int toolIndex);
	void setBackoffParameters(BackoffParameters params, int toolIndex);
	
	class PIDParameters {
		public float p;
		public float i;
		public float d;
	}
	
	PIDParameters getPIDParameters(int which, int toolIndex);
	void setPIDParameters(int which, PIDParameters params, int toolIndex);
	
	class ExtraFeatures {
		final static int CHA = 0;
		final static int CHB = 1;
		final static int CHC = 2;
		public boolean swapMotorController;
		public int heaterChannel;
		public int hbpChannel;
		public int abpChannel;
	}
	
	ExtraFeatures getExtraFeatures(int toolIndex);
	void setExtraFeatures(ExtraFeatures features, int toolIndex);
	
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

	/** Reset the onboard parameters on the motherboard to factory settings. 
	 * @throws RetryException */ 
	void resetSettingsToFactory() throws RetryException;

	/** reset the onboard params to be totally blank */
	void resetSettingsToBlank() throws RetryException;

	/** Reset the onboard parameters on the extruder controller to factory settings. */ 
	void resetToolToFactory(int toolIndex);
	void resetToolToBlank(int toolIndex);

	
	boolean hasVrefSupport();
	/** set the Stepper Voltage Reference */
	void setStoredStepperVoltage(int stepperId, int referenceValue);
	/** get the Stepper Voltage Reference */
	int getStoredStepperVoltage(int stepperId);
	
	public class CommunicationStatistics {
		public int packetCount;
		public int sentPacketCount;
		public int packetFailureCount;
		public int packetRetryCount;
		public int noiseByteCount;
	}
	
	CommunicationStatistics getCommunicationStatistics();

	String getMachineType();

	/// Returns the number of tools as saved on the machine (not as per XML count)
	int toolCountOnboard();

	/// Returns true of tool count is save on the machine  (not as per XML count)
	boolean hasToolCountOnboard();

	/// Sets the number of tool count as saved on the machine (not as per XML count)
	void setToolCountOnboard(int i);
	
	boolean hasHbp();
		//See if eeprom has hbp_exists defined
	public void setHbpSetting(boolean on_off);
		//set the eeprom HBP setting
	byte currentHbpSetting();
		//Reads eeprom returns if HBP is on or off
}
