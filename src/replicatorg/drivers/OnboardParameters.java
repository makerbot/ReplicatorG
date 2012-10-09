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

	// NOTE: A number of these offsets also exist in Sanguino3GDriver.java

	public enum EEPROMParams {
		ABP_COPIES,                 // Number of copies to print with ABP
		ACCEL_ADVANCE_K,            // JKN Advance K
		ACCEL_ADVANCE_K2,           // JKN Advance K2
		ACCEL_CLOCKWISE_EXTRUDER,   // Extruder turns clockwise (1) or ccw (0)
		ACCEL_EXTRUDER_DEPRIME_A,   // Extruder deprime (mm or steps)
		ACCEL_EXTRUDER_DEPRIME_B,   // Extruder deprime (mm or steps)
		ACCEL_E_STEPS_PER_MM,       // Extruder steps per mm of output noodle (steps/mm)
		ACCEL_MAX_ACCELERATION_A,   // A axis max acceleration (mm/s^2)
		ACCEL_MAX_ACCELERATION_B,   // A axis max acceleration (mm/s^2)
		ACCEL_MAX_ACCELERATION_X,   // X axis max acceleration (mm/s^2)
		ACCEL_MAX_ACCELERATION_Y,   // Y axis max acceleration (mm/s^2)
		ACCEL_MAX_ACCELERATION_Z,   // Z axis max acceleration (mm/s^2)
		ACCEL_MAX_EXTRUDER_NORM,    // For a normal, non-travel move, the max magnitude of the accel vector (mm/s)
		ACCEL_MAX_EXTRUDER_RETRACT, // For an extruder-only move, the max acceleration (mm/s^2)
		ACCEL_MAX_FEEDRATE_A,       // A axis max feedrate (mm/s)
		ACCEL_MAX_FEEDRATE_B,       // B axis max feedrate (mm/s)
		ACCEL_MAX_FEEDRATE_X,       // X axis max feedrate (mm/s)
		ACCEL_MAX_FEEDRATE_Y,       // Y axis max feedrate (mm/s)
		ACCEL_MAX_FEEDRATE_Z,       // Z axis max feedrate (mm/s)
		ACCEL_MAX_SPEED_CHANGE_A,   // A axis max speed change (mm/s)
		ACCEL_MAX_SPEED_CHANGE_B,   // A axis max speed change (mm/s)
		ACCEL_MAX_SPEED_CHANGE_X,   // X axis max speed change (mm/s)
		ACCEL_MAX_SPEED_CHANGE_Y,   // Y axis max speed change (mm/s)
		ACCEL_MAX_SPEED_CHANGE_Z,   // Z axis max speed change (mm/s)
		ACCEL_MIN_FEED_RATE,        // Min magnitude of the velocity vector (mm/s)
		ACCEL_MIN_PLANNER_SPEED,    // Min magnitude of the velocity vector at segment junctions
		ACCEL_MIN_SEGMENT_TIME,     // Min time to print a segment (seconds)
		ACCEL_MIN_TRAVEL_FEED_RATE, // Min feedrate for a travel-only move (mm/s)
		ACCEL_NOODLE_DIAMETER,      // Diameter of the extruded noodle (mm)
		ACCEL_REV_MAX_FEED_RATE,    // Max feedrate for an extruder only move (mm/s)
		ACCEL_SLOWDOWN_LIMIT,       // Planner slowdown limit (0, 3, 4, 5, 6, 7, 8; Tom & Cupcake)
		ACCEL_SLOWDOWN_FLAG,        // Enable slowdown limit (0, 1; MightyBoard)
		AXIS_HOME_POSITIONS,        //
		AXIS_INVERSION,             //
		BUZZER_REPEATS,             //
		DITTO_PRINT_ENABLED,	    //
		ENDSTOPS_USED,              //
		ENDSTOP_INVERSION,          //
		ESTOP_CONFIGURATION,        //
		EXTRUDE_DURATION,           //
		EXTRUDE_MMS,                //
		FILAMENT_USED,              // Lifetime filament used (steps)
		FILAMENT_USED_TRIP,         // Filament used since trip counter last reset (steps)
		HOMING_FEED_RATE_X,         //
		HOMING_FEED_RATE_Y,         //
		HOMING_FEED_RATE_Z,         //
		INVERTED_EXTRUDER_5D,       // Invert extruder steps to extrude filament (0=no | 1=yes)
		JOG_MODE_SETTINGS,          //
		LCD_TYPE,                   //
		MACHINE_NAME,               // 32 byte, NUL terminated field containing the machine's name
		MOOD_LIGHT_CUSTOM_BLUE,     //
		MOOD_LIGHT_CUSTOM_GREEN,    //
		MOOD_LIGHT_CUSTOM_RED,      //
		MOOD_LIGHT_SCRIPT,          //
		OVERRIDE_GCODE_TEMP,        // Override gcode temp settings with preheat temps
		PLATFORM_TEMP,              // Preheat & Override build platform temperature (C)
		PREHEAT_DURING_ESTIMATE,    // Start preheating while estimating build time
		PREHEAT_DURING_PAUSE,       // Preheat during pause
		RAM_USAGE_DEBUG,            // SRAM highwater mark
		STEPPER_DRIVER,             // Bit 0: accel driver on/off, bit 1: planner on/off, bit 2: strangled on/off
		STEPS_PER_MM_A,             // A axis steps per millimeter (steps/mm)
		STEPS_PER_MM_B,             // B axis steps per millimeter (steps/mm)
		STEPS_PER_MM_X,             // X axis steps per millimeter (steps/mm)
		STEPS_PER_MM_Y,             // Y axis steps per millimeter (steps/mm)
		STEPS_PER_MM_Z,             // Z axis steps per millimeter (steps/mm)
		TOOL0_TEMP,                 // Preheat & Override tool 0 temperature (C)
		TOOL1_TEMP,                 // Preheat & Override tool 1 temperature (C)
		VERSION_HIGH,               //
		VERSION_LOW,                //
	};

	////.setValue(0);
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
	String getDriverName();

	/// The following are used by the MightyBoard which
	///    1. Tends to use int16_t and uint16_t for many of its onboard params
	///    2. Uses a 16 bit signed fixed point for storing non-integer values

        /// Store acceleration settings to eeprom 
        void setAccelerationStatus(byte status);
        void setAccelerationRate(int rate);
        void setAxisAccelerationRate(int axis, int rate);
        void setAxisJerk(int axis, double jerk);
        void setAccelerationMinimumSpeed(int speed);
        /// Read acceleration settings from eeprom
	byte getAccelerationStatus();
        int getAccelerationRate();
        int getAxisAccelerationRate(int axis);
        double getAxisJerk(int axis);
        int getAccelerationMinimumSpeed();

	/// The following are used by the Gen3/Gen4 boards with the Jetty Firmware
	///    1. Tends to use uint32_t but also int8_t and uint8_t
	///    2. Uses a uint32_t to store floating point values; the exponent
	///       for each value is unique to the value being stored but NOT part
	///       of the stored value (it's hardcoded by the consuming code)

	// For int32_t, int8_t and uint8_t
	int getEEPROMParamInt(EEPROMParams param);

	// For uint32_t
	long getEEPROMParamUInt(EEPROMParams param);

	// For floating point types stored in a uint32_t
	double getEEPROMParamFloat(EEPROMParams param);

	// For int32_t, int8_t, and uint8_t
	void setEEPROMParam(EEPROMParams param, int value);

	// For uint32_t
	void setEEPROMParam(EEPROMParams param, long value);
        
	// For floating point types stored in EEPROM as a uint32_t
	void setEEPROMParam(EEPROMParams param, double value);

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

	/// Returns true if the accelerated driver is Jetty flavour
	boolean hasJettyAcceleration();
	/// Returns true if the driver has the advanced reporting feature set
	boolean hasAdvancedFeatures();
}
