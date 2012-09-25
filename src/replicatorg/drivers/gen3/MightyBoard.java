/*
 MightyBoard.java

 This is a driver to control a machine that uses the MightyBoard electronics.

 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package replicatorg.drivers.gen3;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.logging.Level;

import javax.vecmath.Point3d;

import replicatorg.app.Base;
import replicatorg.drivers.InteractiveDisplay;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.Version;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolheadsOffset;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;

/// EEPROM offsets for PID electronics control
final class PIDTermOffsets implements EEPROMClass {
	final static int P_TERM_OFFSET = 0x0000;
	final static int I_TERM_OFFSET = 0x0002;
	final static int D_TERM_OFFSET = 0x0004;
};


/// EEPROM offset class for toolhead info/data
class ToolheadEEPROM implements EEPROMClass
{
	////Feature map: 2 bytes
	public static final int FEATURES				= 0x0000;
	/// Backoff stop time, in ms: 2 bytes
	public static final int BACKOFF_STOP_TIME         = 0x0002;
	/// Backoff reverse time, in ms: 2 bytes
	public static final int BACKOFF_REVERSE_TIME      = 0x0004;
	/// Backoff forward time, in ms: 2 bytes
	public static final int BACKOFF_FORWARD_TIME      = 0x0006;
	/// Backoff trigger time, in ms: 2 bytes
	public static final int BACKOFF_TRIGGER_TIME      = 0x0008;
	/// Extruder heater base location: 6 bytes
	public static final int EXTRUDER_PID_BASE         = 0x000A;
	/// HBP heater base location: 6 bytes data
	public static final int HBP_PID_BASE              = 0x0010;
	/// Extra features word: 2 bytes
	public static final int EXTRA_FEATURES            = 0x0016;
	/// Extruder identifier; defaults to 0: 1 byte 
	/// Padding: 1 byte of space
	public static final int SLAVE_ID                  = 0x0018;
	/// Cooling fan info: 2 bytes 
	public static final int COOLING_FAN_SETTINGS 	= 0x001A;
	/// Padding: 6 empty bytes of space
	
	// TOTAL MEMORY SIZE PER TOOLHEAD = 28 bytes
}


class MightyBoard5XEEPROM implements EEPROMClass
{
	/// EEPROM for Mightyboard in the 4.x and 5.x firmware version
	/// NOTE: this file needs to match the data in EepromMap.hh for all 
	/// version of firmware for the specified machine. IE, all MightyBoard firmware
	/// must be compatible with this eeprom map. 
	
	
	/// Misc info values
	public static final int EEPROM_CHECK_LOW = 0x5A;
	public static final int EEPROM_CHECK_HIGH = 0x78;
	
	public static final int MAX_MACHINE_NAME_LEN = 16; // set to 32 in firmware
	
	final static class ECThermistorOffsets {
	
		final public static int R0 = 0x00;
		final public static int T0 = 0x04;
		final public static int BETA = 0x08;
		final public static int DATA = 0x10;
		
		public static int r0(int which) { return R0 + THERM_TABLE; }
		public static int t0(int which) { return T0 + THERM_TABLE; }
		public static int beta(int which) { return BETA + THERM_TABLE; }
		public static int data(int which) { return DATA + THERM_TABLE; }
	};
        
        final static class AccelerationOffsets {
	
		final public static int Active = 0x00;
		final public static int Rate = 0x02;
		final public static int AxisRate = 0x04;
		final public static int AxisJerk = 0x0E;
                final public static int MinimumSpeed = 0x18;
                final public static int DefaultsFlag = 0x1A;

	};
	
	
	
	/// Version, low byte: 1 byte
	final public static int VERSION_LOW				= 0x0000;
	/// Version, high byte: 1 byte
	final public static int VERSION_HIGH				= 0x0001;
	/// Axis inversion flags: 1 byte.
	/// Axis N (where X=0, Y=1, etc.) is inverted if the Nth bit is set.
	/// Bit 7 is used for HoldZ OFF: 1 = off, 0 = on
	final public static int AXIS_INVERSION			= 0x0002;
	/// Endstop inversion flags: 1 byte.
	/// The endstops for axis N (where X=0, Y=1, etc.) are considered
	/// to be logically inverted if the Nth bit is set.
	/// Bit 7 is set to indicate endstops are present; it is zero to indicate
	/// that endstops are not present.
	/// Ordinary endstops (H21LOB et. al.) are inverted.
	/// only valid values are 0x9F, 0x80 or 0x00
	final public static int ENDSTOP_INVERSION			= 0x0004;
	/// Digital Potentiometer Settings : 5 Bytes
	final public static int DIGI_POT_SETTINGS			= 0x0006;
	/// axis home direction (1 byte)
	final public static int AXIS_HOME_DIRECTION = 0x000C;
	/// Default locations for the axis in step counts: 5 x 32 bit = 20 bytes
	final public static int AXIS_HOME_POSITIONS		= 0x000E;
	/// Name of this machine: 32 bytes.
	final public static int MACHINE_NAME			= 0x0022;
	/// Tool count : 2 bytes
	final public static int TOOL_COUNT 				= 0x0042;
	/// Hardware ID. Must exactly match the USB VendorId/ProductId pair: 4Bytes 
	final public static int VID_PID_INFO			= 0x0044; 
	/// 44 bytes padding
	/// Thermistor table 0: 128 bytes
	final public static int THERM_TABLE				= 0x0074;
	/// Padding: 8 bytes
	// Toolhead 0 data: 26 bytes (see above)
	final public static int T0_DATA_BASE			= 0x0100;
	// Toolhead 0 data: 26 bytes (see above)
	final public static int T1_DATA_BASE			= 0x011C;
	/// 2 bytes padding
	/// Light Effect table. 3 Bytes x 3 entries
	final public static int LED_STRIP_SETTINGS		= 0x0140;
	/// Buzz Effect table. 4 Bytes x 3 entries
	/// 1 byte padding for offsets
	final public static int BUZZ_SETTINGS		= 0x014A;
	///  1 byte. 0x01 for 'never booted before' 0x00 for 'have been booted before)
	final public static int FIRST_BOOT_FLAG	= 0x0156;
    /// 7 bytes, short int x 3 entries, 1 byte on/off
    final public static int PREHEAT_SETTINGS = 0x0158;
    /// 1 byte,  0x01 for help menus on, 0x00 for off
    final public static int FILAMENT_HELP_SETTINGS = 0x0160;
    /// This indicates how far out of tolerance the toolhead0 toolhead1 distance is
    /// in steps.  3 x 32 bits = 12 bytes
    final public static int TOOLHEAD_OFFSET_SETTINGS = 0x0162;
    /// Acceleraton settings 1byte + 2 bytes
    final public static int ACCELERATION_SETTINGS = 0x016E;
	/// axis lengths XYZ AB 5*32bit = 20 bytes
	final public static int AXIS_LENGTHS		= 0x018C;
	/// axis steps per mm XYZAB 5*32bit = 20 bytes
	final public static int AXIS_STEPS_PER_MM         = 0x01A0;
	/// axis max feedrates XYZAB 5*32bit = 20 bytes
	final public static int AXIS_MAX_FEEDRATES         = 0x027A;

    /// start of free space
    final public static int FREE_EEPROM_STARTS = 0x028E;
}



class MightyBoard6XEEPROM implements EEPROMClass
{
	/// EEPROM for Mightyboard in the 4.x and 5.x firmware version
	/// NOTE: this file needs to match the data in EepromMap.hh for all 
	/// version of firmware for the specified machine. IE, all MightyBoard firmware
	/// must be compatible with this eeprom map. 
	
	
	/// Misc info values
	public static final int EEPROM_CHECK_LOW = 0x5A;
	public static final int EEPROM_CHECK_HIGH = 0x78;
	
	public static final int MAX_MACHINE_NAME_LEN = 16; // set to 32 in firmware
	
	final static class ECThermistorOffsets {
	
		final public static int R0 = 0x00;
		final public static int T0 = 0x04;
		final public static int BETA = 0x08;
		final public static int DATA = 0x10;
		
		public static int r0(int which) { return R0 + THERM_TABLE; }
		public static int t0(int which) { return T0 + THERM_TABLE; }
		public static int beta(int which) { return BETA + THERM_TABLE; }
		public static int data(int which) { return DATA + THERM_TABLE; }
	};
        
        final static class AccelerationOffsets {
	
		final public static int Active = 0x00;
		final public static int Rate = 0x02;
		final public static int AxisRate = 0x04;
		final public static int AxisJerk = 0x0E;
                final public static int MinimumSpeed = 0x18;
                final public static int DefaultsFlag = 0x1A;
		
	};
	
	
	
	/// Version, low byte: 1 byte
	final public static int VERSION_LOW				= 0x0000;
	/// Version, high byte: 1 byte
	final public static int VERSION_HIGH				= 0x0001;
	/// Axis inversion flags: 1 byte.
	/// Axis N (where X=0, Y=1, etc.) is inverted if the Nth bit is set.
	/// Bit 7 is used for HoldZ OFF: 1 = off, 0 = on
	final public static int AXIS_INVERSION			= 0x0002;
	/// Endstop inversion flags: 1 byte.
	/// The endstops for axis N (where X=0, Y=1, etc.) are considered
	/// to be logically inverted if the Nth bit is set.
	/// Bit 7 is set to indicate endstops are present; it is zero to indicate
	/// that endstops are not present.
	/// Ordinary endstops (H21LOB et. al.) are inverted.
	/// only valid values are 0x9F, 0x80 or 0x00
	final public static int ENDSTOP_INVERSION			= 0x0004;
	/// Digital Potentiometer Settings : 5 Bytes
	final public static int DIGI_POT_SETTINGS			= 0x0006;
	/// axis home direction (1 byte)
	final public static int AXIS_HOME_DIRECTION = 0x000C;
	/// Default locations for the axis in step counts: 5 x 32 bit = 20 bytes
	final public static int AXIS_HOME_POSITIONS		= 0x000E;
	/// Name of this machine: 32 bytes.
	final public static int MACHINE_NAME			= 0x0022;
	/// Tool count : 2 bytes
	final public static int TOOL_COUNT 				= 0x0042;
	/// Hardware ID. Must exactly match the USB VendorId/ProductId pair: 4Bytes 
	final public static int VID_PID_INFO			= 0x0044; 
	/// Version number to be tagged with Git Commit
	//  two bytes
	final public static int INTERNAL_VERSION 		= 0X0048;
	/// Git Commit number
	//  two bytes
	final public static int COMMIT_VERSION 			= 0X004A;
	/// Boolean if HBP exists
	//  two bytes
	final public static int HBP_PRESENT 			= 0X004C;
	/// 38 bytes padding
	/// Thermistor table 0: 128 bytes	
	final public static int THERM_TABLE				= 0x0074;
	/// Padding: 8 bytes
	// Toolhead 0 data: 26 bytes (see above)
	final public static int T0_DATA_BASE			= 0x0100;
	// Toolhead 0 data: 26 bytes (see above)
	final public static int T1_DATA_BASE			= 0x011C;
	/// axis lengths (mm) (6 bytes)
	//8 bytes padding. Was formerly AXIS_LENGTHS	= 0x0138;
	/// 2 bytes padding
	/// Light Effect table. 3 Bytes x 3 entries
	final public static int LED_STRIP_SETTINGS		= 0x0140;
	/// Buzz Effect table. 4 Bytes x 3 entries
	/// 1 byte padding for offsets
	final public static int BUZZ_SETTINGS			= 0x014A;
	///  1 byte. 0x01 for 'never booted before' 0x00 for 'have been booted before)
	final public static int FIRST_BOOT_FLAG				= 0x0156;
    /// 7 bytes, short int x 3 entries, 1 byte on/off
    final public static int PREHEAT_SETTINGS 			= 0x0158;
    /// 1 byte,  0x01 for help menus on, 0x00 for off
    final public static int FILAMENT_HELP_SETTINGS		= 0x0160;
    /// This indicates how far out of tolerance the toolhead0 toolhead1 distance is
    /// in steps.  3 x 32 bits = 12 bytes
    final public static int TOOLHEAD_OFFSET_SETTINGS	= 0x0162;
    /// Acceleration settings 1byte + 2 bytes
    final public static int ACCELERATION_SETTINGS		= 0x016E;

    /// Last know bot status, 
    /// 2 bytes
    final public static int BOT_STATUS_BYTE				= 0x018A;
    /// axis lengths XYZ AB 5*32bit = 20 bytes 
    final public static int AXIS_LENGTHS             	= 0x018C; 

    /// Estimated lifetime print hours, 3bytes
    final public static int TOTAL_BUILD_TIME			= 0x01A0;
    /// Hardware configuration settings 
    final public static int BOTSTEP_TYPE      			= 0x01A4;
    /// start of free space
  	final public static int FREE_EEPROM_STARTS			= 0x01A6;

}



/**
 * Object for managing the connection to the MightyBoard hardware.
 * @author FarMcKon
 */
public class MightyBoard extends Makerbot4GAlternateDriver
	implements InteractiveDisplay
	//implements OnboardParameters, SDCardCapture
{
	// note: other machines also inherit: PenPlotter, MultiTool
	
	/// Stores LED color by effect. Mostly uses '0' (color now)
	private Hashtable ledColorByEffect;

	/// Stores the last know stepper values.
	/// on boot, fetches those values from the machine,
	/// afterwords updated when stepper values are set (currently
	/// there is no way to get stepper values from the machine)
	/// hash is <int(StepperId), int(StepperLastSetValue>
	private Hashtable stepperValues; //only 0 - 127 are valid

	
	/// 0 -127, current reference value. Store on desktop for this machine
	private int voltageReference; 

	private boolean eepromChecked = false;
	
	protected final static int DEFAULT_RETRIES = 5;
	
	protected VidPid machineId = VidPid.UNKNOWN;
	protected int toolCountOnboard = -1; /// no count aka FFFF
	
	protected Version toolVersion = new Version(0,0);
	protected Version accelerationVersion = new Version(0,0);
	protected Version minAdvancedFeatureVersion = new Version(6,0); // Earliest firmware version with 'advanced' features. See footnote[1]

	/** 
	 * Standard Constructor
	 */
	public MightyBoard() {
		super();
		ledColorByEffect = new Hashtable();
		ledColorByEffect.put(0, Color.BLACK);
		Base.logger.info("Created a MightyBoard");

		stepperValues= new Hashtable();
		
		// Make sure this accurately reflects the minimum preferred
		// firmware version we want this driver to support.
		minimumVersion = new Version(5,6);
		preferredVersion = new Version(5,6);
		minimumAccelerationVersion = new Version(5,3);
		minAdvancedFeatureVersion = new Version(6,0);
		minimumJettyAccelerationVersion = new Version(5,6);
	}

	public String getDriverName() {
		return "MightyBoard";
	}
	
	/**
	 * Initalize the extruder or sub-controllers.
	 * For mightyboard, this involves setting some tool values,
	 * and checking some eeprom values
	 * @param toolIndex
	 * @return
	 */
	public boolean initSlave(int toolIndex)
	{
		// since our motor speed is controlled by host software,
		// initalize 'running' motor speed to be the same as the 
		// default motor speed
		ToolModel curTool = machine.getTool(toolIndex);
		curTool.setMotorSpeedReadingRPM( curTool.getMotorSpeedRPM() );
		return true;
	}

	/**
	 * This function is called just after a connection is made, to do initial
	 * sycn of any values stored in software that are not available by 
	 * s3g command. For example, stepper vRef
	 * @see replicatorg.drivers.gen3.Sanguino3GDriver#pullInitialValuesFromBot()
	 */
	@Override
	public boolean initializeBot()
	{
		// Scan for each slave
		for (ToolModel t : getMachine().getTools()) {
			if (t != null) {
				initSlave(t.getIndex());
			}
		}

		getStepperValues(); //read our current steppers into a local cache
		getMotorRPM();		//load our motor RPM from firmware if we can.
		getAccelerationState();

		if (verifyMachineId() == false ) //read and verify our PID/VID if we can
		{
			Base.logger.fine("Machine ID Mismatch. Please re-select your machine.");
			return true;//TEST just for now, due to EEPROM mangling
		}
		
		if(verifyToolCount() == false) /// read and verify our tool count
		{
			Base.logger.severe("Tool Count Mismatch. Expecting "+ machine.getTools().size() + " tools, reported " + this.toolCountOnboard + "tools");
			Base.logger.severe("Please double-check your selected machine.");
		}
		
		// I have no idea why we still do this, we may want to test and refactor away
		getSpindleSpeedPWM();

		// Check the steps per mm and axis lengths stored in the firmware for the XYZAB axis, and if they
		// don't match the machine definition, write them and reset the bot
//		boolean needsReset = checkAndWriteStepsPerMM();
//		needsReset |= checkAndWriteAxisLengths();
//		needsReset |= checkAndWriteMaxFeedRates();
//		if ( needsReset ) {
//               		setInitialized(true);	//Needed to get a proper reset
//			reset();
//               		setInitialized(false);
//		}

		return true;
	}
	
        
	/// Read acceleration OFF/ON status from Bot
	private void getAccelerationState(){
	    
	    Base.logger.fine("Geting Acceleration Status from Bot");
	    acceleratedFirmware = getAccelerationStatus() != 0;
	    if(acceleratedFirmware)
	        Base.logger.finest("Found accelerated firmware active");
	    
	}

	
	/// Read stepper reference voltage values from the Bot's EEPROM.
	private void getStepperValues() {
		
		int stepperCountMightyBoard = 5;
		Base.logger.fine("MightyBoard initial Sync");
		for(int i = 0; i < stepperCountMightyBoard; i++)
		{
			int vRef = getStoredStepperVoltage(i); 
			Base.logger.fine("Caching inital Stepper vRef from bot");
			Base.logger.finer("Stepper i = " + i + " vRef =" + vRef);
			stepperValues.put(new Integer(i), new Integer(vRef) );
		}
	}

	/**
	 * 
	 *  Sets the reference voltage for the specified stepper. This will change the reference voltage
	 *  and therefore the power used by the stepper. Voltage range is 0v (0) to 1.7v (127) for Replicator machine
	 * @param stepperId target stepper index
	 * @param referenceValue the reference voltage value, from 0-127
	 * @throws RetryException
	 */
	@Override
	public void setStepperVoltage(int stepperId, int referenceValue) throws RetryException {
		Base.logger.fine("MightyBoard sending setStepperVoltage: " + stepperId + " " + referenceValue);
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_STEPPER_REFERENCE_POT.getCode());
		
		if(stepperId > 5) {
			Base.logger.severe("set invalid stepper Id" + Integer.toString(stepperId) );
			return; 
		}
		if (referenceValue > 127) 	 referenceValue= 127; 
		else if (referenceValue < 0) referenceValue= 0;
		
		pb.add8(stepperId);
		pb.add8(referenceValue); //range should be only is 0-127
		PacketResponse pr = runCommand(pb.getPacket());

		if( pr.isOK() )
		{
			stepperValues.put(new Integer(stepperId), new Integer(referenceValue));
		}
	}

	@Override
	public int getStepperVoltage(int stepperId )
	{
		Integer key = new Integer(stepperId);
		if( stepperValues.containsKey(key) ){
			Integer stepperVal = (Integer)stepperValues.get(key);
			return (int)stepperVal;
		}

		Base.logger.severe("No known local stepperVoltage: " + stepperId);
		return 0;
	}


	// Checks the steps per mm stored in the firmware for all axis, and updates them to
	// match the ones stored in the machine xml if they are different

	public boolean checkAndWriteStepsPerMM() {

		if (!hasJettyAcceleration())
			return false;

		Point5d machineStepsPerMM = getMachine().getStepsPerMM();

		boolean needsReset = false;
		int stepperCountMightyBoard = 5;
		for(int i = 0; i < stepperCountMightyBoard; i++) {
			double firmwareAxisStepsPerMM = read32FromEEPROM(MightyBoard5XEEPROM.AXIS_STEPS_PER_MM + i*4);

			double val = 0.0;

			switch (i) {
				case 0:
					val = machineStepsPerMM.x();
					break;
				case 1:
					val = machineStepsPerMM.y();
					break;
				case 2:
					val = machineStepsPerMM.z();
					break;
				case 3:
					val = machineStepsPerMM.a();
					break;
				case 4:
					val = machineStepsPerMM.b();
					break;

			}
	
			val = (int)(val * 1000000.0);

			if ( firmwareAxisStepsPerMM != val ) {
				Base.logger.info("Bot StepsPerMM Axis " + i + ": " + firmwareAxisStepsPerMM / 1000000.0 + 
						 " machine xml has: " + val / 1000000.0+ ", updating bot");
				write32ToEEPROM32(MightyBoard5XEEPROM.AXIS_STEPS_PER_MM + i*4, (int)val);
				needsReset = true;
			}
		}

		return needsReset;
	}


	// Checks the max feed rates stored in the firmware for all axis, and updates them to
	// match the ones stored in the machine xml if they are different

	public boolean checkAndWriteMaxFeedRates() {

		if (!hasJettyAcceleration())
			return false;

		Point5d maximumFeedRates = getMachine().getMaximumFeedrates();

		boolean needsReset = false;
		int stepperCountMightyBoard = 5;
		for(int i = 0; i < stepperCountMightyBoard; i++) {
			double firmwareAxisMaximumFeedRate = read32FromEEPROM(MightyBoard5XEEPROM.AXIS_MAX_FEEDRATES + i*4);

			double val = 0.0;

			switch (i) {
				case 0:
					val = maximumFeedRates.x();
					break;
				case 1:
					val = maximumFeedRates.y();
					break;
				case 2:
					val = maximumFeedRates.z();
					break;
				case 3:
					val = maximumFeedRates.a();
					break;
				case 4:
					val = maximumFeedRates.b();
					break;

			}
	
			if ( firmwareAxisMaximumFeedRate != val ) {
				Base.logger.info("Bot Maximum Feed Rate Axis " + i + ": " + firmwareAxisMaximumFeedRate + 
						 " machine xml has: " + val + ", updating bot");
				write32ToEEPROM32(MightyBoard5XEEPROM.AXIS_MAX_FEEDRATES + i*4, (int)val);
				needsReset = true;
			}
		}

		return needsReset;
	}


	// Checks the axis lengths stored in the firmware for all axis, and updates them to
	// match the ones stored in the machine xml if they are different

	public boolean checkAndWriteAxisLengths() {

		if (!hasJettyAcceleration())
			return false;

		Point5d axisLengths = getMachine().getAxisLengths();
		Point5d machineStepsPerMM = getMachine().getStepsPerMM();

		boolean needsReset = false;
		int stepperCountMightyBoard = 5;
		for(int i = 0; i < stepperCountMightyBoard; i++) {
			int firmwareAxisLength = read32FromEEPROM(MightyBoard5XEEPROM.AXIS_LENGTHS + i*4);

			int val = 0;

			switch (i) {
				case 0:
					val = (int)(axisLengths.x() * machineStepsPerMM.x());
					break;
				case 1:
					val = (int)(axisLengths.y() * machineStepsPerMM.y());
					break;
				case 2:
					val = (int)(axisLengths.z() * machineStepsPerMM.z());
					break;
				case 3:
					val = (int)(axisLengths.a() * machineStepsPerMM.a());
					break;
				case 4:
					val = (int)(axisLengths.b() * machineStepsPerMM.b());
					break;

			}
	
			if ( firmwareAxisLength != val ) {
				Base.logger.info("Bot Length Axis " + i + ": " + firmwareAxisLength + 
						 " machine xml has: " + val + ", updating bot");
				write32ToEEPROM32(MightyBoard5XEEPROM.AXIS_LENGTHS + i*4, val);
				needsReset = true;
			}
		}

		return needsReset;
	}

	
	@Override
	public boolean hasVrefSupport() {
		return true;
	}
	
	
	
	@Override
	public int getStoredStepperVoltage(int stepperId) 
	{
		Base.logger.fine("Getting stored stepperVoltage: " + stepperId );
		int vRefForPotLocation = MightyBoard5XEEPROM.DIGI_POT_SETTINGS + stepperId;
		
		Base.logger.finer("Getting stored stepperVoltage from eeprom addr: " + vRefForPotLocation  );

		byte[] voltages = readFromEEPROM(vRefForPotLocation, 1) ;
		if(voltages == null ) {
			Base.logger.severe("null response to EEPROM read at "+ vRefForPotLocation);
			return 0;
		}

		if(voltages[0] > 127)		voltages[0] = 127;
		else if(voltages[0] < 0)	voltages[0] = 0;

		return (int)voltages[0];
		
	}

	@Override
	public void queuePoint(final Point5d p) throws RetryException {

		/*
		 * So, it looks like points specified in A/E/B commands turn in the opposite direction from
		 * turning based on tool RPM
		 * 
		 * I recieve all points as absolute values, and, really, all extruder values should be sent
		 * as relative values, just in case we end up with an overflow?
		 *
		 */
		Point5d target = new Point5d(p);
		Point5d current = new Point5d(getPosition());
		
		// is this point even step-worthy? Only compute nonzero moves
		Point5d deltaSteps = getAbsDeltaSteps(current, target);
		if (deltaSteps.length() > 0.0) {
			// relative motion in mm
			Point5d deltaMM = new Point5d();
			deltaMM.sub(target, current); // delta = p - current
			
			// A and B are always sent as relative, rec'd as absolute, so adjust our target accordingly
			// Also, our machine turns the wrong way? make it negative.
			target.setA(-deltaMM.a());
			target.setB(-deltaMM.b());

			// calculate the time to make the move
			Point5d delta3d = new Point5d();
			delta3d.setX(deltaMM.x());
			delta3d.setY(deltaMM.y());
			delta3d.setZ(deltaMM.z());
			double distance = delta3d.distance(new Point5d());
			double feedrate = getSafeFeedrate(deltaMM);	//Feedrate in mm/min
			double minutes = distance / feedrate;
			
			// if minutes == 0 here, we know that this is just an extrusion in place
			// so we need to figure out how long it will take
			if(minutes == 0) {
				distance = Math.max(Math.abs(deltaMM.a()), Math.abs(deltaMM.b()));
				minutes = distance / feedrate;
			}

			//convert feedrate to mm/sec
			feedrate = feedrate / 60.0;
			
			Point5d stepsPerMM = machine.getStepsPerMM();
			
			// if either a or b is 0, but their motor is on, create a distance for them
			if(deltaMM.a() == 0) {
				ToolModel aTool = extruderHijackedMap.get(AxisId.A);
				if(aTool != null && aTool.isMotorEnabled()) {
					// minute * revolution/minute
					double numRevolutions = minutes * aTool.getMotorSpeedRPM();
					// steps/revolution * mm/steps 	
					double mmPerRevolution = aTool.getMotorSteps() * (1/stepsPerMM.a());
					// set distance
					target.setA( -(numRevolutions * mmPerRevolution));
				}
			}
			if(deltaMM.b() == 0) {
				ToolModel bTool = extruderHijackedMap.get(AxisId.B);
				if(bTool != null && bTool.isMotorEnabled()) {
					// minute * revolution/minute
					double numRevolutions = minutes * bTool.getMotorSpeedRPM();
					// steps/revolution * mm/steps 	
					double mmPerRevolution = bTool.getMotorSteps() * (1/stepsPerMM.b());
					// set distance
					target.setB( -(numRevolutions * mmPerRevolution));
				}
			}
			
			// calculate absolute position of target in steps
			Point5d excess = new Point5d(stepExcess);
			Point5d steps = machine.mmToSteps(target,excess);	
			
			double usec = (60 * 1000 * 1000 * minutes);

			//Convert usec into dda_interval
			//Theoretically we shouldn't get a divide by zero scenario
			//due to the "if (deltaSteps.length() > 0.0)" above.
			//We need to get the delta steps again because they could have changed
			current.setA(0);	//Because A is a relative move
			current.setB(0);	//Because B is a relative move
			Point5d deltaStepsFinal = getAbsDeltaSteps(current, target);
			double dda_interval = usec / deltaStepsFinal.absolute_maximum();

			//Convert dda_interval into dda_rate (dda steps per second on the master axis)
			double dda_rate = 1000000d / dda_interval;

			//System.out.println(p.toString());
			//System.out.println(target.toString());
			//System.out.println("\t steps: " + steps.toString() +"\t dda_rate: " + dda_rate);
			//System.out.println("\t usec: " + usec + " dda_interval: " + dda_interval + " absolute_maximum: " + steps.absolute_maximum());
			//System.out.println("\t deltaSteps: " + deltaStepsFinal.toString() + " distance: " + distance);
			int relativeAxes = (1 << AxisId.A.getIndex()) | (1 << AxisId.B.getIndex());
			queueNewExtPoint(steps, (long) dda_rate, relativeAxes, (float)distance, (float)feedrate);

			// Only update excess if no retry was thrown.
			stepExcess = excess;

			// because of the hinky stuff we've been doing with A & B axes, just pretend we've
			// moved where we thought we were moving
			Point5d fakeOut = new Point5d(target);
			fakeOut.setA(p.a());
			fakeOut.setB(p.b());
			setInternalPosition(fakeOut);
		}
	}
	
	@Override
	public void setStoredStepperVoltage(int stepperId, int referenceValue) {
		Base.logger.finer("MightyBoard sending storeStepperVoltage");

		if(stepperId > 5) {
			Base.logger.severe("store invalid stepper Id" + Integer.toString(stepperId) );
			return; 
		}
		if (referenceValue > 127)		referenceValue= 127; 
		else if (referenceValue < 0)	referenceValue= 0; 

		int vRefForPotLocation = MightyBoard5XEEPROM.DIGI_POT_SETTINGS + stepperId;
		byte b[] = new byte[1];
		b[0] =  (byte)referenceValue;
		checkEEPROM();
		writeToEEPROM(vRefForPotLocation, b);
	}
	

	protected byte getColorBits(Color inputColor){
		byte bitfield = 0x00;
		int red = inputColor.getRed();
		int green = inputColor.getGreen();
		int blue = inputColor.getBlue();
		//craptastic. Now converting annoying RGB ints to 
		// a bitfiled with crazy signed bytes.
		red= (red >> 6);
		green= (green>> 6);
		blue = blue >> 6;
		bitfield |= (byte)(blue << 4);
		bitfield |= (byte)(green << 2);
		bitfield |= (byte)(red );
		// {bits: XXBBGGRR : BLUE: 0b110000, Green:0b1100, RED:0b11}
		return bitfield;
	}
	/**
	 * Sends a command to the 3d printer to set it's LEDs.   Sets color, and possible effect flag
	 * @param color The desired color to set the leds to
	 * @param effectId The effect for the LED to set.  NOTE: some effects do not immedately change colors, but
	 * 		store color information for later use.  Zero indicates 'set color immedately'
	 * @throws RetryException
	 */
	@Override
	public void setLedStrip(Color color, int effectId) throws RetryException {
		Base.logger.fine("MightyBoard sending setLedStrip");

	/*	PacketBuilder pb1 = new PacketBuilder(MotherboardCommandCode.SET_LED_STRIP_COLOR.getCode());
		pb1.add8(3);//color.getRed());
		pb1.add8(0);//color.getBlue());
		pb1.add8(0);//color.getGreen());
		pb1.add8(0xFF);
		pb1.add8(0);
		runCommand(pb1.getPacket());
*/
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_LED_STRIP_COLOR.getCode());

		int Channel = 3;
		int Brightness = 1;
		int BlinkRate = 0;
		byte colorSelect = (byte)0x3F;
       
       // {bits: XXBBGGRR : BLUE: 0b110000, Green:0b1100, RED:0b11}
       colorSelect = getColorBits(color);
       
		pb.add8(color.getRed());
		pb.add8(color.getGreen());
		pb.add8(color.getBlue());	
		pb.add8(BlinkRate);
		//pb.add8(colorSelect);
		pb.add8(0);

		PacketResponse resp =  runCommand(pb.getPacket());
		if(resp.isOK()) {
			Base.logger.fine("MightyBoard setLedStrip went OK");
			ledColorByEffect.put(effectId, color);	
		}
	}
	

	/**
	 * Sends a beep command to the bot. The beep will sound immedately
	 * @param frequencyHz frequency of the beep
	 * @param duration how long the beep will sound in ms
	 * @param effects The beep effect, TBD. NOTE: some effects do not immedately change colors, but
	 * 		store color information for later use. Zero indicates 'sound beep immedately'
	 * @throws RetryException
	 */
	public void sendBeep(int frequencyHz, int durationMs, int effectId) throws RetryException {
		Base.logger.fine("MightyBoard sending setBeep" + frequencyHz + durationMs + " effect" + effectId);
		Base.logger.fine("max " + Integer.MAX_VALUE);
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_BEEP.getCode());
		pb.add16(frequencyHz);
		pb.add16(durationMs);
		pb.add8(effectId);		
		PacketResponse resp =  runCommand(pb.getPacket());
		if(resp.isOK()) {
			Base.logger.fine("MightyBoard sendBeep went OK");
			//beepByEffect.put(effectId, color);	
		}

	}	
	
	private void checkEEPROM() {
		if (!eepromChecked) {
			// Versions 2 and up have onboard eeprom defaults and rely on 0xff values
			eepromChecked = true;
			if (version.getMajor() < 2) {
				byte versionBytes[] = readFromEEPROM(MightyBoard5XEEPROM.VERSION_LOW,2);
				if (versionBytes == null || versionBytes.length < 2) 
					return;
				if ((versionBytes[0] != MightyBoard5XEEPROM.EEPROM_CHECK_LOW) || 
					(versionBytes[1] != MightyBoard5XEEPROM.EEPROM_CHECK_HIGH)) 
				{
					Base.logger.severe("Cleaning EEPROM to v1.X state");
					// Wipe EEPROM
					byte eepromWipe[] = new byte[16];
					Arrays.fill(eepromWipe,(byte)0x00);
					eepromWipe[0] = MightyBoard5XEEPROM.EEPROM_CHECK_LOW;
					eepromWipe[1] = MightyBoard5XEEPROM.EEPROM_CHECK_HIGH;
					writeToEEPROM(0,eepromWipe);
					Arrays.fill(eepromWipe,(byte)0x00);
					for (int i = 16; i < 256; i+=16) {
						writeToEEPROM(i,eepromWipe);
					}
				}
				Base.logger.severe("checkEEPROM has version" + version.toString());
			}
		}
	}
	
	@Override
	public EnumSet<AxisId> getInvertedAxes() {
		checkEEPROM();
		byte[] b = readFromEEPROM(MightyBoard5XEEPROM.AXIS_INVERSION,1);
		EnumSet<AxisId> r = EnumSet.noneOf(AxisId.class);
		if(b != null) {
			if ( (b[0] & (0x01 << 0)) != 0 ) r.add(AxisId.X);
			if ( (b[0] & (0x01 << 1)) != 0 ) r.add(AxisId.Y);
			if ( (b[0] & (0x01 << 2)) != 0 ) r.add(AxisId.Z);
			if ( (b[0] & (0x01 << 3)) != 0 ) r.add(AxisId.A);
			if ( (b[0] & (0x01 << 4)) != 0 ) r.add(AxisId.B);
			if ( (b[0] & (0x01 << 7)) != 0 ) r.add(AxisId.V);
			return r;
		}
		Base.logger.severe("Null settings for getInvertedParameters");
		return EnumSet.noneOf(AxisId.class);	
	}

	@Override
	public void setInvertedAxes(EnumSet<AxisId> axes) {
		byte b[] = new byte[1];
		if (axes.contains(AxisId.X)) b[0] = (byte)(b[0] | (0x01 << 0));
		if (axes.contains(AxisId.Y)) b[0] = (byte)(b[0] | (0x01 << 1));
		if (axes.contains(AxisId.Z)) b[0] = (byte)(b[0] | (0x01 << 2));
		if (axes.contains(AxisId.A)) b[0] = (byte)(b[0] | (0x01 << 3));
		if (axes.contains(AxisId.B)) b[0] = (byte)(b[0] | (0x01 << 4));
		if (axes.contains(AxisId.V)) b[0] = (byte)(b[0] | (0x01 << 7));
		writeToEEPROM(MightyBoard5XEEPROM.AXIS_INVERSION,b);
	}

	@Override
	public String getMachineName() {

		if( botName != null )
			return botName; 
		
		checkEEPROM();
		
		byte[] data = readFromEEPROM(MightyBoard5XEEPROM.MACHINE_NAME,
				MightyBoard5XEEPROM.MAX_MACHINE_NAME_LEN);

		if (data == null)
			{ return "no name"; }
		try {
			int len = 0;
			while (len < MightyBoard5XEEPROM.MAX_MACHINE_NAME_LEN && data[len] != 0) len++;
			String name = new String(data,0,len,"ISO-8859-1");
			this.botName = name;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return this.botName; ///may be null if name set failed
	}


	public void setMachineName(String machineName) {
		int maxLen = MightyBoard5XEEPROM.MAX_MACHINE_NAME_LEN;
		machineName = new String(machineName);
		if (machineName.length() > maxLen) { 
			machineName = machineName.substring(0,maxLen);
		}
		byte b[] = new byte[maxLen];
		int idx = 0;
		for (byte sb : machineName.getBytes()) {
			b[idx++] = sb;
			if (idx == maxLen) break;
		}
		if (idx < maxLen) b[idx] = 0;
		writeToEEPROM(MightyBoard5XEEPROM.MACHINE_NAME,b);
	}
	

	@Override
	public double getAxisHomeOffset(int axis) {

		Base.logger.finest("MigtyBoard getAxisHomeOffset" + axis);
		if ((axis < 0) || (axis > 4)) {
			// TODO: handle this
			Base.logger.severe("axis out of range" + axis);
			return 0;
		}
		
		checkEEPROM();

		double val = read32FromEEPROM(MightyBoard5XEEPROM.AXIS_HOME_POSITIONS + axis*4);

		Point5d stepsPerMM = getMachine().getStepsPerMM();
		switch(axis) {
			case 0:
				val = val/stepsPerMM.x();
				break;
			case 1:
				val = val/stepsPerMM.y();
				break;
			case 2:
				val = val/stepsPerMM.z();
				break;
			case 3:
				val = val/stepsPerMM.a();
				break;
			case 4:
				val = val/stepsPerMM.b();
				break;
		}
		
		
		return val;
	}

	
	@Override
	public void setAxisHomeOffset(int axis, double offset) {
		if ((axis < 0) || (axis > 4)) {
			// TODO: handle this
			return;
		}
		
		int offsetSteps = 0;
		
		Point5d stepsPerMM = getMachine().getStepsPerMM();
		switch(axis) {
			case 0:
				offsetSteps = (int)(offset*stepsPerMM.x());
				break;
			case 1:
				offsetSteps = (int)(offset*stepsPerMM.y());
				break;
			case 2:
				offsetSteps = (int)(offset*stepsPerMM.z());
				break;
			case 3:
				offsetSteps = (int)(offset*stepsPerMM.a());
				break;
			case 4:
				offsetSteps = (int)(offset*stepsPerMM.b());
				break;
		}
		write32ToEEPROM32(MightyBoard5XEEPROM.AXIS_HOME_POSITIONS + axis*4,offsetSteps);
	}

	@Override
	public boolean hasToolheadsOffset() {
		if (machine.getTools().size() == 1)	return false;
		return true;
	}

	@Override
	public double getToolheadsOffset(int axis) {

		Base.logger.finest("MightyBoard getToolheadsOffset" + axis);
		if ((axis < 0) || (axis > 2)) {
			// TODO: handle this
			Base.logger.severe("axis out of range" + axis);
			return 0;
		}
		
		checkEEPROM();

		double val = read32FromEEPROM(MightyBoard5XEEPROM.TOOLHEAD_OFFSET_SETTINGS + axis*4);

		ToolheadsOffset toolheadsOffset = getMachine().getToolheadsOffsets();
		Point5d stepsPerMM = getMachine().getStepsPerMM();
		switch(axis) {
			case 0:
				val = (val)/stepsPerMM.x()/10.0 + toolheadsOffset.x();
				break;
			case 1:
				val = (val)/stepsPerMM.y()/10.0 + toolheadsOffset.y();
				break;
			case 2:
				val = (val)/stepsPerMM.z()/10.0 + toolheadsOffset.z();
				break;
		}
				
		return val;
	}

	/// Looks up a key value based on the machine setting/status.
	/// Only used for getting baseline acceleration values for
	// Print-O-Matic
	@Override 
	public String getConfigValue(String key, String baseline)
	{
		//Base.logger.severe("MightyBoard fetching from getConfig");
		if( this.getAccelerationStatus() != 0 ) {
			//Base.logger.severe("MightyBoard is accel");
			if ( key.equals("desiredFeedrate")  )  return "80";
			if ( key.equals("travelFeedrate") )    return "150";
			if ( key.equals("printTemp") )    		return "240";
			
		} else  {
			//Base.logger.severe("MightyBoard is not accel");
			if ( key.equals("desiredFeedrate")  )  return "40";
			if ( key.equals("travelFeedrate") )    return "55";
			if ( key.equals("printTemp") )    		return "220";
		}
		return baseline;
	}
	
	/**
	 * Stores to EEPROM in motor steps counts, how far out of 
	 * tolerance the toolhead0 to toolhead1 distance is. XML settings are used
	 * to calculate expected distance to sublect to tolerance error from.
	 * @param axis axis to store 
	 * @param distanceMm total distance of measured offset, tool0 to too1
	 */
	@Override
	public void eepromStoreToolDelta(int axis, double distanceMm) {
		if ((axis < 0) || (axis > 2)) {
			// TODO: handle this
			return;
		}
		
		int offsetSteps = 0;
		
		Point5d stepsPerMM = getMachine().getStepsPerMM();
		ToolheadsOffset toolheadsOffset = getMachine().getToolheadsOffsets();
		
		switch(axis) {
			case 0:
				offsetSteps = (int)((distanceMm-toolheadsOffset.x())*stepsPerMM.x()*10.0);
				break;
			case 1:
				offsetSteps = (int)((distanceMm-toolheadsOffset.y())*stepsPerMM.y()*10.0);
				break;
			case 2:
				offsetSteps = (int)((distanceMm-toolheadsOffset.z())*stepsPerMM.z()*10.0);
				break;
		}
		write32ToEEPROM32(MightyBoard5XEEPROM.TOOLHEAD_OFFSET_SETTINGS + axis*4,offsetSteps);
	}
        
        @Override
	/// WARNING: MightyBoard 5.5 and earlier
        /// get stored acceleration rate from bot
        /// acceleration rate is applied to all moves when acceleration is active in firmware
        public int getAccelerationRate(){
                
                Base.logger.finest("MightyBoard getAccelerationRate" );
		
		checkEEPROM();

		int val = read16FromEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS + 
                        MightyBoard5XEEPROM.AccelerationOffsets.Rate);
				
		return val;
        }
        
        @Override
	/// WARNING: MightyBoard 5.5 and earlier
        /// set acceleration rate store on bot
        /// acceleration rate is applied to all moves when acceleration is active in firmware
        public void setAccelerationRate(int rate){
            
            Base.logger.finest("MightyBoard setAccelerationRate" );

            // limit rate to 16 bit integer max
            if(rate  > 32767)
                rate = 32767;
            if(rate < -32768)
                rate = -32768;
                
            write16ToEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS + 
                    MightyBoard5XEEPROM.AccelerationOffsets.Rate, rate);
        }
        
        /**
	 * WARNING: MightyBoard 5.5 and earlier
	 * Reads the individual acceleration rates of each axis,
	 * in mm/s.
	 */
	@Override
	public int getAxisAccelerationRate(int axis) {
		checkEEPROM();
                int val = read16FromEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS +
                        MightyBoard5XEEPROM.AccelerationOffsets.AxisRate + (axis * 2));

		return val;
	}

	/**
	 * WARNING: MightyBoard 5.5 and earlier
	 * Writes the individual acceleration rates of each axis,
	 * in mm/s, to the motherboard EEPROM.
	 */
	@Override
	public void setAxisAccelerationRate(int axis, int rate) {

		write16ToEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS + 
                        MightyBoard5XEEPROM.AccelerationOffsets.AxisRate + (axis * 2), rate);
	}
        
        /**
	 * WARNING: MightyBoard 5.5 and earlier
	 * Reads the individual acceleration rates of each axis,
	 * in mm/s.
	 */
	@Override
	public int getAccelerationMinimumSpeed() {
		checkEEPROM();
                int val = read16FromEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS + 
                        MightyBoard5XEEPROM.AccelerationOffsets.MinimumSpeed);

		return val;
	}

	/**
	 * WARNING: MightyBoard 5.5 and earlier
	 * Writes the individual acceleration rates of each axis,
	 * in mm/s, to the motherboard EEPROM.
	 */
	@Override
	public void setAccelerationMinimumSpeed(int speed) {

		write16ToEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS + 
                        MightyBoard5XEEPROM.AccelerationOffsets.MinimumSpeed, speed);
	}
        
        /**
	 * WARNING: MightyBoard 5.5 and earlier
	 * Reads the individual acceleration rates of each axis,
	 * in mm/s.
	 */
	@Override
	public double getAxisJerk(int axis) {
		checkEEPROM();
                byte[] stored = readFromEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS + 
                        MightyBoard5XEEPROM.AccelerationOffsets.AxisJerk + (axis * 2),2);
                
                double val = byte16LEToFloat(stored);
		return val;
	}

	/**
	 * WARNING: MightyBoard 5.5 and earlier
	 * Writes the individual acceleration rates of each axis,
	 * in mm/s, to the motherboard EEPROM.
	 */
	@Override
	public void setAxisJerk(int axis, double jerk) {

		writeToEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS + 
                        MightyBoard5XEEPROM.AccelerationOffsets.AxisJerk + (axis * 2), floatToLE((float)jerk));
	}

        
  @Override
  // get stored acceleration status:
//    bit 0:  OFF (0) or ON (1)
  public byte getAccelerationStatus(){
          
    Base.logger.finest("MightyBoard getAccelerationStatus");

    checkEEPROM();

		byte[] val;

		if (hasJettyAcceleration())
			val = readFromEEPROM(JettyMBEEPROM.ACCELERATION_STATE, 1);
		else
			val = readFromEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS,1);

		return val[0];
  }

  @Override
  // set stored acceleration status: either ON of OFF
  // acceleration is applied to all moves, except homing when ON
  public void setAccelerationStatus(byte status){
      Base.logger.info("MightyBoard setAccelerationStatus");
      
      byte b[] = new byte[1];
      b[0] = status;

	    if (hasJettyAcceleration())
		    writeToEEPROM(JettyMBEEPROM.ACCELERATION_STATE, b);
	    else
		    writeToEEPROM(MightyBoard5XEEPROM.ACCELERATION_SETTINGS, b);
   }
        
	public void createThermistorTable(int which, double r0, double t0, double beta) {
		// Generate a thermistor table for r0 = 100K.
		final int ADC_RANGE = 1024;
		final int NUMTEMPS = 20;
		byte table[] = new byte[NUMTEMPS*2*2];
		class ThermistorConverter {
			final double ZERO_C_IN_KELVIN = 273.15;
			public double vadc,rs,vs,k,beta;
			public ThermistorConverter(double r0, double t0C, double beta, double r2) {
				this.beta = beta;
				this.vs = this.vadc = 5.0;
				final double t0K = ZERO_C_IN_KELVIN + t0C;
				this.k = r0 * Math.exp(-beta / t0K);
				this.rs = r2;		
			}
			public double temp(double adc) {
				// Convert ADC reading into a temperature in Celsius
				double v = adc * this.vadc / ADC_RANGE;
				double r = this.rs * v / (this.vs - v);
				return (this.beta / Math.log(r / this.k)) - ZERO_C_IN_KELVIN;
			}
		};
		ThermistorConverter tc = new ThermistorConverter(r0,t0,beta,4700.0);
		double adc = 1; // matching the python script's choices for now;
		// we could do better with this distribution.
		for (int i = 0; i < NUMTEMPS; i++) {
			double temp = tc.temp(adc);
			// extruder controller is little-endian
			int tempi = (int)temp;
			int adci = (int)adc;
			Base.logger.fine("{ "+Integer.toString(adci) +"," +Integer.toString(tempi)+" }");
			table[(2*2*i)+0] = (byte)(adci & 0xff); // ADC low
			table[(2*2*i)+1] = (byte)(adci >> 8); // ADC high
			table[(2*2*i)+2] = (byte)(tempi & 0xff); // temp low
			table[(2*2*i)+3] = (byte)(tempi >> 8); // temp high
			adc += (ADC_RANGE/(NUMTEMPS-1));
		}
		// Add indicators
		byte eepromIndicator[] = new byte[2];
		eepromIndicator[0] = MightyBoard5XEEPROM.EEPROM_CHECK_LOW;
		eepromIndicator[1] = MightyBoard5XEEPROM.EEPROM_CHECK_HIGH;
		writeToToolEEPROM(0,eepromIndicator);

		writeToEEPROM(MightyBoard5XEEPROM.ECThermistorOffsets.beta(which),intToLE((int)beta));
		writeToEEPROM(MightyBoard5XEEPROM.ECThermistorOffsets.r0(which),intToLE((int)r0));
		writeToEEPROM(MightyBoard5XEEPROM.ECThermistorOffsets.t0(which),intToLE((int)t0));
		writeToEEPROM(MightyBoard5XEEPROM.ECThermistorOffsets.data(which),table);
	}
	
	/**
	 * 
	 * @param which if 0 this is the extruder, if 1 it's the HBP attached to the extruder
	 */
	@Override
	public int getBeta(int which, int toolIndex) {
		Base.logger.severe("beta for " + Integer.toString(toolIndex));
		byte r[] = readFromEEPROM(MightyBoard5XEEPROM.ECThermistorOffsets.beta(which),4);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}
	
	@Override
	public EndstopType getInvertedEndstops() {
		checkEEPROM();
		byte[] b =  readFromEEPROM(MightyBoard5XEEPROM.ENDSTOP_INVERSION,1);
		return EndstopType.endstopTypeForValue(b[0]);
	}

	@Override
	public void setInvertedEndstops(EndstopType endstops) {
		byte b[] = new byte[1];
		b[0] = endstops.getValue();
		writeToEEPROM(MightyBoard5XEEPROM.ENDSTOP_INVERSION,b);
	}

	@Override
	public ExtraFeatures getExtraFeatures(int toolIndex) {
		Base.logger.severe("extra Feat: " + Integer.toString(toolIndex));
		int efdat = read16FromToolEEPROM(ToolheadEEPROM.FEATURES ,0x4084, toolIndex);
		ExtraFeatures ef = new ExtraFeatures();
		ef.swapMotorController = (efdat & 0x0001) != 0;
		ef.heaterChannel = (efdat >> 2) & 0x0003;
		ef.hbpChannel = (efdat >> 4) & 0x0003;
		ef.abpChannel = (efdat >> 6) & 0x0003;
//		System.err.println("Extra features: smc "+Boolean.toString(ef.swapMotorController));
//		System.err.println("Extra features: ch ext "+Integer.toString(ef.heaterChannel));
//		System.err.println("Extra features: ch hbp "+Integer.toString(ef.hbpChannel));
//		System.err.println("Extra features: ch abp "+Integer.toString(ef.abpChannel));
		return ef;
	}
	
	public void setExtraFeatures(ExtraFeatures features) {
		int efdat = 0x4000;
		if (features.swapMotorController) { efdat = efdat | 0x0001; }
		efdat |= features.heaterChannel << 2;
		efdat |= features.hbpChannel << 4;
		efdat |= features.abpChannel << 6;
		//System.err.println("Writing to EF: "+Integer.toHexString(efdat));
		writeToToolEEPROM(ToolheadEEPROM.FEATURES, intToLE(efdat,2));
	}

	@Override
	public EstopType getEstopConfig() {
		checkEEPROM();
		byte[] b = readFromEEPROM(MightyBoard5XEEPROM.ENDSTOP_INVERSION,1);
		return EstopType.estopTypeForValue(b[0]);
	}

	@Override
	public void setEstopConfig(EstopType estop) {
		byte b[] = new byte[1];
		b[0] = estop.getValue();
		writeToEEPROM(MightyBoard5XEEPROM.ENDSTOP_INVERSION,b);
	}
	
	
		/// Check the EEPROM to see what PID/VID the machine believes it has
	public void readMachineVidPid() {
		checkEEPROM();
		byte[] b = readFromEEPROM(MightyBoard5XEEPROM.VID_PID_INFO,4);
		this.machineId = VidPid.getPidVid(b);
	}
	
	/// Function to grab cached count of tools
	@Override
	public int toolCountOnboard() { 
		return getToolheadCount(); //forward to the (new) fetch from board function 
		} 

	
	public boolean verifyToolCount()
	{
		readToolheadCount(); 
		if(this.toolCountOnboard ==  machine.getTools().size())
			return true;
		return false;


	}
	
	/** try to verify our acutal machine matches our selected machine
	 * @param vid vendorId (same as usb vendorId)
	 * @param pid product (same as usb productId)
	 * @return true if we can verify this is a valid machine match
	 */
	@Override
	public boolean verifyMachineId()
	{
		if ( this.machineId == VidPid.UNKNOWN ) {
			readMachineVidPid();
		}
		if(this.machineId.equals(VidPid.MIGHTY_BOARD)) {
			Base.logger.severe("You are running an unverified MightyBoard,");
			Base.logger.severe("your machine is not a verified MakerBot Replicator");			
			return true;
		}
		return this.machineId.equals(VidPid.THE_REPLICATOR);
	}

	@Override
	public boolean canVerifyMachine() {
		return true;
	}


	@Override
	public boolean setConnectedToolIndex(int index) {
		byte[] data = new byte[1];
		data[0] = (byte) index;
		Base.logger.severe("setConnectedToolIndex not supported in MightyBoard");
		
		//throw new UnsupportedOperationException("setConnectedToolIndex not supported in MightyBoard");
		
		// The broadcast address has changed. The safest solution is to try both.
		//writeToToolEEPROM(MightyBoard5XEEPROM.EC_EEPROM_SLAVE_ID, data, 255); //old firmware used 255, new fw ignores this
		//writeToToolEEPROM(MightyBoard5XEEPROM.EC_EEPROM_SLAVE_ID, data, 127); //new firmware used 127, old fw ignores this
		return false;
	}


	@Override
	@Deprecated
	protected void writeToToolEEPROM(int offset, byte[] data) {
		writeToToolEEPROM(offset, data, machine.currentTool().getIndex());
	}
	

	@Override
	protected void writeToToolEEPROM(int offset, byte[] data, int toolIndex) {
		final int MAX_PAYLOAD = 11;
		while (data.length > MAX_PAYLOAD) {
			byte[] head = new byte[MAX_PAYLOAD];
			byte[] tail = new byte[data.length-MAX_PAYLOAD];
			System.arraycopy(data,0,head,0,MAX_PAYLOAD);
			System.arraycopy(data,MAX_PAYLOAD,tail,0,data.length-MAX_PAYLOAD);
			writeToToolEEPROM(offset, head, toolIndex);
			offset += MAX_PAYLOAD;
			data = tail;
		}
		PacketBuilder slavepb = new PacketBuilder(MotherboardCommandCode.TOOL_QUERY.getCode());
		slavepb.add8((byte) toolIndex);
		slavepb.add8(ToolCommandCode.WRITE_TO_EEPROM.getCode());
		slavepb.add16(offset);
		slavepb.add8(data.length);
		for (byte b : data) {
			slavepb.add8(b);
		}
		PacketResponse slavepr = runQuery(slavepb.getPacket());
		slavepr.printDebug();
		// If the tool index is 127/255, we should not expect a response (it's a broadcast packet).
		assert (toolIndex == 255) || (toolIndex == 127) || (slavepr.get8() == data.length); 
	}

	
	/**
	 * Reads a chunk of data from the tool EEPROM. 
	 * For mightyboard, this data is stored onbopard
	 * @param offset  offset into the 'Tool' section of the EEPROM
	 * 	(the location of the tool section of eeprom is calculated in this function')
	 */
	@Override 
	protected byte[] readFromToolEEPROM(int offset, int len, int toolIndex) 
	{

		int toolInfoOffset = 0;
		if (toolIndex == 0)	toolInfoOffset = MightyBoard5XEEPROM.T0_DATA_BASE;
		else if (toolIndex == 1)toolInfoOffset = MightyBoard5XEEPROM.T1_DATA_BASE;

		offset = toolInfoOffset + offset;
		Base.logger.finest("readFromToolEEPROM null" + offset +" " + len + " " + toolIndex);
				
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.READ_EEPROM.getCode());
		pb.add16(offset);
		pb.add8(len);
		PacketResponse pr = runQuery(pb.getPacket());
		if (pr.isOK()) {
			Base.logger.finest("readFromToolEEPROM ok at: " + offset +" len:" + len + " id:" + toolIndex);			
			//Base.logger.severe("readFromToolEEPROM ok");
			int rvlen = Math.min(pr.getPayload().length - 1, len);
			byte[] rv = new byte[rvlen];
			// Copy removes the first response byte from the packet payload.
			System.arraycopy(pr.getPayload(), 1, rv, 0, rvlen);
			return rv;
		} else {
			Base.logger.severe("On tool read: " + pr.getResponseCode().getMessage());
		}
		Base.logger.severe("readFromToolEEPROM null" + offset +" " + len + " " + toolIndex);
		return null;
	}

	

	/** 
	 * Enable extruder motor
	 */
	@Override
	public void enableMotor(int toolhead) throws RetryException {

		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		
		//WARNING: this in unsafe, since tool is checked
		//async from when command is set. Tool should be a param
		ToolModel curTool = machine.getTool(toolhead);
		Iterable<AxisId>  axes = getHijackedAxes(curTool);

		// Hack conversion to match datapoints. ToDo: convert all to Interable or all to EnumSet, 
		// stop using a mix
		EnumSet<AxisId> axesEnum = EnumSet.noneOf(AxisId.class);
		for( AxisId e: axes)
			axesEnum.add(e);

		enableAxes(axesEnum);
		curTool.enableMotor();
	}

	/** 
	 * Disable our extruder motor
	 */
	@Override
	public void disableMotor(int toolhead) throws RetryException {

		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();
		
		ToolModel curTool = machine.getTool(toolhead);//WARNING: this in unsafe, since tool is checked

		//async from when command is set. Tool should be a param
		Iterable<AxisId> axes = getHijackedAxes(curTool);

		// Hack conversion to match datapoints. ToDo: convert all to Interable or all to EnumSet, 
		// stop using a mix
		EnumSet<AxisId> axesEnum = EnumSet.noneOf(AxisId.class);
		for( AxisId e: axes)
			axesEnum.add(e);

		disableAxes(axesEnum);
		curTool.disableMotor();
	}

	
	/*****************************
	 * Overrides for all users of readFromToolEEPROM from Sanguino3GdDriver,
	 * MakerBot4GDriver and MakerBot3GAlternateDriver
	 *******************************/
	@Override
	public boolean getCoolingFanEnabled(int toolIndex) {
		Base.logger.severe("getCoolingFanEnable: " + Integer.toString(toolIndex));
		byte[]  a = readFromToolEEPROM(ToolheadEEPROM.COOLING_FAN_SETTINGS, 1, toolIndex);
		return (a[0] == 1);
	}

	/**
	 * 
	 * @param which if 0 this is the extruder, if 1 it's the HBP attached to the extruder
	 */
	@Override
	public int getR0(int which, int toolIndex) {
		Base.logger.severe("getR0: " + Integer.toString(toolIndex));
		byte r[] = readFromEEPROM(MightyBoard5XEEPROM.ECThermistorOffsets.r0(which),4);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}

	/**
	 * 
	 * @param which if 0 this is the extruder, if 1 it's the HBP attached to the extruder
	 */
	@Override
	public int getT0(int which, int toolIndex) {
		Base.logger.severe("getT0: " + Integer.toString(toolIndex));
		byte r[] = readFromEEPROM(MightyBoard5XEEPROM.ECThermistorOffsets.t0(which),4);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}

	@Override
	@Deprecated
	protected int read16FromToolEEPROM(int offset, int defaultValue) {
		return read16FromToolEEPROM(offset, defaultValue, machine.currentTool().getIndex());
	}
	
	@Override
	protected int read16FromToolEEPROM(int offset, int defaultValue, int toolIndex) {
		byte r[] = readFromToolEEPROM(offset, 2, toolIndex);
		int val = ((int) r[0]) & 0xff;
		Base.logger.severe("val " + val + " & " + (((int) r[1]) & 0xff) );
		val += (((int) r[1]) & 0xff) << 8;
		if (val == 0x0ffff) {
			Base.logger.fine("ERROR: Eeprom val at "+ offset +" is 0xFFFF");
			return defaultValue;
		}
		return val;
	}


	@Override
	public BackoffParameters getBackoffParameters(int toolIndex) {
		BackoffParameters bp = new BackoffParameters();
		Base.logger.severe("backoff Forward: " + Integer.toString(toolIndex));
		bp.forwardMs = read16FromToolEEPROM( ToolheadEEPROM.BACKOFF_FORWARD_TIME, 300, toolIndex);
		Base.logger.severe("backoff sop: " + Integer.toString(toolIndex));
		bp.stopMs = read16FromToolEEPROM( ToolheadEEPROM.BACKOFF_STOP_TIME, 5, toolIndex);
		Base.logger.severe("backoff reverse: " + Integer.toString(toolIndex));
		bp.reverseMs = read16FromToolEEPROM(ToolheadEEPROM.BACKOFF_REVERSE_TIME, 500, toolIndex);
		Base.logger.severe("backoff trigger: " + Integer.toString(toolIndex));
		bp.triggerMs = read16FromToolEEPROM(ToolheadEEPROM.BACKOFF_TRIGGER_TIME, 300, toolIndex);
		return bp;
	}
	
	@Override
	public void setBackoffParameters(BackoffParameters bp, int toolIndex) {
		writeToToolEEPROM(ToolheadEEPROM.BACKOFF_FORWARD_TIME,intToLE(bp.forwardMs,2), toolIndex);
		writeToToolEEPROM(ToolheadEEPROM.BACKOFF_STOP_TIME,intToLE(bp.stopMs,2), toolIndex);
		writeToToolEEPROM(ToolheadEEPROM.BACKOFF_REVERSE_TIME,intToLE(bp.reverseMs,2), toolIndex);
		writeToToolEEPROM(ToolheadEEPROM.BACKOFF_TRIGGER_TIME,intToLE(bp.triggerMs,2), toolIndex);
	}


	/**
	 * 
	 */
	@Override
	public PIDParameters getPIDParameters(int which, int toolIndex) {
		PIDParameters pp = new PIDParameters();

		int offset = (which == OnboardParameters.EXTRUDER)?
				ToolheadEEPROM.EXTRUDER_PID_BASE:ToolheadEEPROM.HBP_PID_BASE;
		if (which == OnboardParameters.EXTRUDER)
			Base.logger.finest("** PID FOR ID: Extruder" );
		else
			Base.logger.finest("** PID FOR ID: BuildPlatform" );


		Base.logger.finest("pid p: " + Integer.toString(toolIndex));
		pp.p = readFloat16FromToolEEPROM(offset + PIDTermOffsets.P_TERM_OFFSET, 7.0f, toolIndex);
		Base.logger.finest("pid i: " + Integer.toString(toolIndex));
		pp.i = readFloat16FromToolEEPROM(offset + PIDTermOffsets.I_TERM_OFFSET, 0.325f, toolIndex);
		Base.logger.finest("pid d: " + Integer.toString(toolIndex));
		pp.d = readFloat16FromToolEEPROM(offset + PIDTermOffsets.D_TERM_OFFSET, 36.0f, toolIndex);
		return pp;
	}
	
	@Override
	public void setPIDParameters(int which, PIDParameters pp, int toolIndex) {
		int offset = (which == OnboardParameters.EXTRUDER)?
				ToolheadEEPROM.EXTRUDER_PID_BASE:ToolheadEEPROM.HBP_PID_BASE;
		writeToToolEEPROM(offset+PIDTermOffsets.P_TERM_OFFSET,floatToLE(pp.p),toolIndex);
		writeToToolEEPROM(offset+PIDTermOffsets.I_TERM_OFFSET,floatToLE(pp.i),toolIndex);
		writeToToolEEPROM(offset+PIDTermOffsets.D_TERM_OFFSET,floatToLE(pp.d),toolIndex);
	}
	
	
	/**
	 * Reads a EEPROM value from the mahine
	 * @param offset distance into EEPROM to read
	 * @param defaultValue value to return on error or failure
	 * @param toolIndex index of the tool to read/write a bite from 
	 * @return a float value, the 'defaultValue' if there is an error
	 */
	private float readFloat16FromToolEEPROM(int offset, float defaultValue, int toolIndex) {
		byte r[] = readFromToolEEPROM(offset, 2, toolIndex);

		Base.logger.finest("val " + (((int) r[0]) & 0xff)+ " & " + (((int) r[1]) & 0xff) );
		if (r[0] == (byte) 0xff && r[1] == (byte) 0xff){
			Base.logger.fine("ERROR: Eeprom  float 16 val at "+ offset +" is 0xFFFF");
			return defaultValue;
		}
		return (float) byteToInt(r[0]) + ((float) byteToInt(r[1])) / 256.0f;
	}

	private int byteToInt(byte b) {
		return ((int) b) & 0xff;
	}
	
	
	/// read a 32 bit int from EEPROM at location 'offset'
	private int read32FromEEPROM(int offset)
	{
		int val = 0;
		byte[] r = readFromEEPROM(offset, 4);
		if( r == null || r.length < 4) {
			Base.logger.severe("invalid read from read32FromEEPROM at "+ offset);
			return val;
		}
		for (int i = 0; i < 4; i++)
			val = val + (((int)r[i] & 0xff) << 8*i);
		return val;
	}

	private void write32ToEEPROM32(int offset, int value ) {
		int s = value;
		byte buf[] = new byte[4];
		for (int i = 0; i < 4; i++) {
			buf[i] = (byte) (s & 0xff);
				s = s >>> 8;
		}
		writeToEEPROM(offset,buf);
        }
        
        /// read a 16 bit int from EEPROM at location 'offset'

	/// ACTUALLY, this routine reads a uint16_t.  Say what?  If it were reading a int16_t,
	/// then it would need to test the high bit and then negate the resulting Java int if
	/// the high bit is set....

	private int read16FromEEPROM(int offset)
	{
		int val = 0;
		byte[] r = readFromEEPROM(offset, 2);
		if( r == null || r.length < 2) {
			Base.logger.severe("invalid read from read16FromEEPROM at "+ offset);
			return val;
		}
		for (int i = 0; i < 2; i++)
			val = val + (((int)r[i] & 0xff) << 8*i);
		return val;
	}

	private void write16ToEEPROM(int offset, int value ) {
		int s = value;
		byte buf[] = new byte[2];
		for (int i = 0; i < 2; i++) {
			buf[i] = (byte) (s & 0xff);
				s = s >>> 8;
		}
		writeToEEPROM(offset,buf);
        }
	
	/**
	 * Reset to the factory state. For MightyBoard this 
	 * does NOT write 0xFF to all EEPROM, but instead sends a 
	 * 'wipe and reboot' command. 
	 */
	@Override
	public void resetSettingsToFactory() throws RetryException {
		/// send message to FW to wipe all settings
		/// except home, wipe locations, and single/dual status
		PacketBuilder pb = new PacketBuilder( MotherboardCommandCode.RESET_TO_FACTORY.getCode() );
		pb.add8((byte) 0xFF);
		pb.add8(ToolCommandCode.GET_PLATFORM_SP.getCode());
		PacketResponse pr = runCommand( pb.getPacket() );

	}

	@Override
	public void resetToolToFactory(int toolIndex) {
		/// send message to FW to wipe all settings
		/// except home, wipe locations, and single/dual status
	}
		
	/*
	 * For overrides the EEPROM to null, on reboot eeprom will be repopulated with some 
	 * baseline values. 
	 */
	@Override
	public void resetSettingsToBlank() throws RetryException  {
		Base.logger.finer("resetting to Blank in Sanguino3G");
		byte eepromWipe[] = new byte[16];
		Arrays.fill(eepromWipe, (byte) 0xff);
		for (int i = 0; i < 0x0200; i += 16) {
			writeToEEPROM(i, eepromWipe);
		}
	}
	
	@Override
	public void setExtraFeatures(ExtraFeatures features, int toolIndex) {
		int efdat = 0x4000;
		if (features.swapMotorController) {
			efdat = efdat | 0x0001;
		}
		efdat |= features.heaterChannel << 2;
		efdat |= features.hbpChannel << 4;
		efdat |= features.abpChannel << 6;

		//System.err.println("Writing to EF: "+Integer.toHexString(efdat));
		writeToToolEEPROM(ToolheadEEPROM.EXTRA_FEATURES, intToLE(efdat,2), toolIndex);
	}
	

	@Override
	public double getPlatformTemperatureSetting(int toolhead) {
		PacketBuilder pb = new PacketBuilder(
				MotherboardCommandCode.TOOL_QUERY.getCode());
		pb.add8((byte) toolhead);
		pb.add8(ToolCommandCode.GET_PLATFORM_SP.getCode());
		PacketResponse pr = runQuery(pb.getPacket());
		int sp = pr.get16();
		machine.getTool(toolhead).setPlatformTargetTemperature(sp);
		
		return machine.getTool(toolhead).getPlatformTargetTemperature();
	}
	
	// Display a message on the user interface
	public void displayMessage(double seconds, String message, boolean buttonWait) throws RetryException {
		byte options = 0; //bit 1 true cause the buffer to clear, bit 2 true indicates message complete
		final int MAX_MSG_PER_PACKET = 20;
		int sentTotal = 0; /// set 'clear buffer' flag
		double timeout = 0;
		
		/// send message in 25 char blocks. Set 'clear buffer' on the first,
		/// and set the timeout only on the last block
        /// send message complete on the last block
		while (sentTotal < message.length()) {
			PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.DISPLAY_MESSAGE.getCode());
			
			// if this is the last packet, set timeout and indicate that message is complete
            // set the "wait on button" flag if specified
			if(!(sentTotal + MAX_MSG_PER_PACKET <  message.length())){
				timeout = seconds;
				options |= 0x02;
                if(buttonWait)
                    options |= 0x04;
			}
			if(sentTotal  > 0 ) 
				options |= 0x01; //do not clear flag
			pb.add8(options);		
			/// TODO: add method to specify x and y coordinate. 
			/// x and y coordinates is only processed once for each complete message
			pb.add8(0); // x coordinate
			pb.add8(0); // y coordinate
			pb.add8((int)seconds); // send timeout only on the last packet
			sentTotal += pb.addString(message.substring(sentTotal), MAX_MSG_PER_PACKET);
			runCommand(pb.getPacket());
		}					     
	}
	
	public void sendBuildStartNotification(String buildName, int stepCount)  throws RetryException { 
		final int MAX_MSG_PER_PACKET = 25;
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.BUILD_START_NOTIFICATION.getCode());
		pb.add32(stepCount);
		pb.addString(buildName, MAX_MSG_PER_PACKET);//clips name if it's too big
		runCommand(pb.getPacket());
	}
	
	/**
	 * @param endCode Reason for build end 0 is normal ending, 1 is user cancel, 
	 * 					0xFF is cancel due to error or safety cutoff.
	 * @throws RetryException
	 */
	public void sendBuildEndNotification(int endCode)  throws RetryException {
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.BUILD_END_NOTIFICATION.getCode());
		//BUILD_END_NOTIFICATION(24, "Notify the bot object build is complete."),
		pb.add8(endCode);
		runCommand(pb.getPacket());
	}
	
	///
	public void updateBuildPercent(int percentDone) throws RetryException {
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_BUILD_PERCENT.getCode());
		pb.add8(percentDone);
		pb.add8(0xff);///reserved
		runCommand(pb.getPacket());
	}
	
	/// Tells the bot to queue a pre-canned song.
	public void playSong(int songId) throws RetryException {
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.QUEUE_SONG.getCode());
		pb.add8(songId);
		runCommand(pb.getPacket());
	}
	
	public void userPause(double seconds, boolean resetOnTimeout, int buttonMask) throws RetryException {
		int options = resetOnTimeout?1:0;
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.PAUSE_FOR_BUTTON.getCode());
		pb.add8(0xff); // buttonMask);
		pb.add16((int)seconds);
		pb.add8(options);
		runCommand(pb.getPacket());
	}

	@Override
	public double getTemperatureSetting(int toolhead) {
		PacketBuilder pb = new PacketBuilder(
				MotherboardCommandCode.TOOL_QUERY.getCode());
		pb.add8((byte) toolhead );
		pb.add8(ToolCommandCode.GET_SP.getCode());
		PacketResponse pr = runQuery(pb.getPacket());
		int sp = pr.get16();
		machine.getTool(toolhead).setTargetTemperature(sp);
		
		return machine.getTool(toolhead).getTargetTemperature();
	}
	
	

	public Version getToolVersion() {
		return toolVersion;
	}
	
	@Override
	public void setMotorRPM(double rpm, int toolhead) throws RetryException {
	
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		ToolModel curTool = machine.getTool(toolhead);//WARNING: this in unsafe, since tool is checked

		///TRICKY: fot The Replicator,the firmware no longer handles this command
		// it's all done on host side via 5D command translation.  We just set a local value
		curTool.setMotorSpeedRPM(rpm);
	}
	
	/**
	 * Will wait for first the tool, then the build platform, it exists and
	 * supported. Technically the platform is connected to a tool (extruder
	 * controller) but this information is currently not used by the firmware.
	 * 
	 * timeout is given in seconds. If the tool isn't ready by then, the machine
	 * will continue anyway.
	 */
	public void requestToolChange(int toolhead, int timeout)
			throws RetryException {

		selectTool(toolhead);

		Base.logger.fine("Waiting for tool #" + toolhead);

		// send it!
		if (this.machine.getTool(toolhead).getTargetTemperature() > 0.0) {
			PacketBuilder pb = new PacketBuilder(
					MotherboardCommandCode.WAIT_FOR_TOOL.getCode());
			pb.add8((byte) toolhead);
			pb.add16(100); // delay between master -> slave pings (millis)
			pb.add16(timeout); // timeout before continuing (seconds)
			runCommand(pb.getPacket());
		}

		///hack, since we have one HBP on the Replicator for now, search-grab that and heat it at any toolchange.
		boolean needsToHeatHPB = false; 
		int toolheadWithHBP = -1;
		for(ToolModel t : machine.getTools())
			if( t.hasHeatedPlatform() && t.getPlatformTargetTemperature() > 0.0) {
				toolheadWithHBP = t.getIndex();
				needsToHeatHPB = true;
		}
		
		if(needsToHeatHPB && toolheadWithHBP > -1 ) {
			PacketBuilder pb = new PacketBuilder(
					MotherboardCommandCode.WAIT_FOR_PLATFORM.getCode());
			pb.add8((byte) toolheadWithHBP );
			pb.add16(100); // delay between master -> slave pings (millis)
			pb.add16(timeout); // timeout before continuing (seconds)
			runCommand(pb.getPacket());
		}
		
	}
	
	@Override 
	public String getMachineType(){
		if (this.machineId.equals(VidPid.MIGHTY_BOARD))
			return "MightyBoard"; 
		else if (this.machineId.equals(VidPid.THE_REPLICATOR))
			return "The Replicator"; 
		return "MightyBoard(unverified)"; 			
	} 
	
	
	/// Returns the number of tools as saved on the machine (not as per XML count)
	//@Override 
	public void readToolheadCount() { 

		byte[] toolCountByte = readFromEEPROM(MightyBoard5XEEPROM.TOOL_COUNT, 1) ;
		if (toolCountByte != null && toolCountByte.length > 0 ) {
			toolCountOnboard = toolCountByte[0];
		}

	} 
	
	public int getToolheadCount() {
		if (toolCountOnboard == -1 ) 
			readToolheadCount();
		return toolCountOnboard;
	}


	/// Returns true of tool count is save on the machine  (not as per XML count)
	@Override 
	public boolean hasToolCountOnboard() {return true; }

	/// Sets the number of tool count as saved on the machine (not as per XML count)
	@Override
	public void setToolCountOnboard(int i){ 
		byte b[] = {(byte)-1};
		if (i == 1 ||  i == 2)		
			b[0] = (byte)i;
		writeToEEPROM(MightyBoard5XEEPROM.TOOL_COUNT,b);
		
	}; 

	@Override
	/// for 6.0 or later, we do not use offset system See footnote[2]
	public Point3d getOffset(int i) {
		if(this.version.getMajor() < 6)
			return offsets[i];
		Base.logger.info("offsets deprecated in firmware: " + version.toString());
		return offsets[0];
	}

	@Override
	/// for 6.0 or later, we do not use offset system See footnote[2]
	public void setOffsetX(int offsetSystemNum, double j) {
		if(this.version.getMajor() < 6)
			offsets[offsetSystemNum].x = j;
		Base.logger.info("offsets deprecated in firmware: " + version.toString());
	}

	@Override
	/// for 6.0 or later, we do not use offset system See footnote[2]
	public void setOffsetY(int offsetSystemNum, double j) {
		if(this.version.getMajor() < 6)
			offsets[offsetSystemNum].y = j;
		Base.logger.info("offsets deprecated in firmware: " + version.toString());
	}

	@Override
	/// for 6.0 or later, we do not use offset system See footnote[2]
	public void setOffsetZ(int offsetSystemNum, double j) {
		if(this.version.getMajor() < 6)
			offsets[offsetSystemNum].z = j;
		Base.logger.info("offsets deprecated in firmware: " + version.toString());	
	}


	
	/// read a 32 bit unsigned int from EEPROM at location 'offset'
	private long readUInt32FromEEPROM(int offset)
	{
		byte[] r = readFromEEPROM(offset, 4);
		if( r == null || r.length < 4) {
			Base.logger.severe("invalid read from read32FromEEPROM at "+ offset);
			return 0;
		}
		long val = (long)r[0] & 0xffL;
		val += ((long)r[1] & 0xffL) <<  8;
		val += ((long)r[2] & 0xffL) << 16;
		val += ((long)r[3] & 0xffL) << 24;
		return val;
	}

	private void writeUInt32ToEEPROM(int offset, long value) {
		int v;
		// WARNING: you want these L's.  A naked 0xffffffff the int known as -1
		if (value > 0xffffffffL)
			v = 0xffffffff;
		else if (value > 0L)
			v = (int)(0xffffffffL & value);
		else
			v = 0;
		write32ToEEPROM32(offset, v);
	}


	/// Get a stored unsigned 8bit int from EEPROM
	/// Made difficult because Java lacks an unsigned byte and thus when converting from
	/// Byte to Int, the value can go unexpectedly negative and change the bits

	private int getUInt8EEPROM(int offset) {
		byte[] val = readFromEEPROM(offset, 1);
		int i = ( val[0] & 0x7f) + (((0x80 & val[0]) != 0) ? (int)0x80 : (int)0);
		return i;
	}

	/// Write an unsigned 8bit value to EEPROM
	/// We IGNORE the sign bit in the Int: we do not negate the 8bit value (since it's supposed
	/// to be unsigned, eh?).  And, if the value is larger than 0xff we set it to 0xff.  That
	/// way if someone, for instance, enters a temp of 256 we store 255 rather than 0.

	private void setUInt8EEPROM(int offset, int val) {
		byte b[] = new byte[1];
		if (val > 0xff)
			val = 0xff;
		b[0] = (byte)(0xff & val);
		writeToEEPROM(offset, b);
	}

	/// Get a stored 32bit unsigned int from EEPROM

	private long getUInt32EEPROM(int offset) {
		return readUInt32FromEEPROM(offset);
	}

	/// Store a 32bit unsigned int to EEPROM
	private void setUInt32EEPROM(int offset, long val) {
		writeUInt32ToEEPROM(offset, val);
	}

	// The "Int" EEPROM parameters are actually uint8_t (aka, unsigned char) or uint16_t
	// There's no useful equivalent in Java so we promote these to Int

	@Override
	public int getEEPROMParamInt(EEPROMParams param) {
		switch (param) {
		case ACCEL_EXTRUDER_DEPRIME_A   : return read16FromEEPROM(JettyMBEEPROM.EXTRUDER_DEPRIME_STEPS + 0*2);
		case ACCEL_EXTRUDER_DEPRIME_B   : return read16FromEEPROM(JettyMBEEPROM.EXTRUDER_DEPRIME_STEPS + 1*2);
		case ACCEL_MAX_ACCELERATION_A   : return read16FromEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 3*2);
		case ACCEL_MAX_ACCELERATION_B   : return read16FromEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 4*2);
		case ACCEL_MAX_ACCELERATION_X   : return read16FromEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 0*2);
		case ACCEL_MAX_ACCELERATION_Y   : return read16FromEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 1*2);
		case ACCEL_MAX_ACCELERATION_Z   : return read16FromEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 2*2);
		case ACCEL_MAX_EXTRUDER_NORM    : return read16FromEEPROM(JettyMBEEPROM.MAX_ACCELERATION_NORMAL_MOVE);
		case ACCEL_MAX_EXTRUDER_RETRACT : return read16FromEEPROM(JettyMBEEPROM.MAX_ACCELERATION_EXTRUDER_MOVE);
		case ACCEL_MAX_SPEED_CHANGE_A   : return read16FromEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 3*2);
		case ACCEL_MAX_SPEED_CHANGE_B   : return read16FromEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 4*2);
		case ACCEL_MAX_SPEED_CHANGE_X   : return read16FromEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 0*2);
		case ACCEL_MAX_SPEED_CHANGE_Y   : return read16FromEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 1*2);
		case ACCEL_MAX_SPEED_CHANGE_Z   : return read16FromEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 2*2);
		case ACCEL_SLOWDOWN_FLAG        : return getUInt8EEPROM(JettyMBEEPROM.SLOWDOWN_FLAG);
		case PREHEAT_DURING_PAUSE       : return getUInt8EEPROM(JettyMBEEPROM.HEAT_DURING_PAUSE);
		case OVERRIDE_GCODE_TEMP        : return getUInt8EEPROM(JettyMBEEPROM.OVERRIDE_GCODE_TEMP);
		default :
			Base.logger.log(Level.WARNING, "getEEPROMParamInt(" + param + ") call failed");
			return 0;
		}
	}

	@Override
	public long getEEPROMParamUInt(EEPROMParams param) {
		switch (param) {
		default :
			Base.logger.log(Level.WARNING, "getEEPROMParamUInt(" + param + ") call failed");
			return 0L;
		}
	}

	@Override
	public double getEEPROMParamFloat(EEPROMParams param) {
		switch (param) {
		case ACCEL_ADVANCE_K            : return (double)getUInt32EEPROM(JettyMBEEPROM.JKN_ADVANCE_K) / 100000.0d;
		case ACCEL_ADVANCE_K2           : return (double)getUInt32EEPROM(JettyMBEEPROM.JKN_ADVANCE_K2) / 100000.0d;
		default :
			Base.logger.log(Level.WARNING, "getEEPROMParamFloat(" + param + ") call failed");
			return 0d;
		}
	}

	@Override
	public void setEEPROMParam(EEPROMParams param, int val) {
		if (val < 0)
			val = 0;
		switch (param) {
		case ACCEL_EXTRUDER_DEPRIME_A   : write16ToEEPROM(JettyMBEEPROM.EXTRUDER_DEPRIME_STEPS + 0*2, val); break;
		case ACCEL_EXTRUDER_DEPRIME_B   : write16ToEEPROM(JettyMBEEPROM.EXTRUDER_DEPRIME_STEPS + 1*2, val); break;
		case ACCEL_MAX_ACCELERATION_A   : write16ToEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 3*2, val); break;
		case ACCEL_MAX_ACCELERATION_B   : write16ToEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 4*2, val); break;
		case ACCEL_MAX_ACCELERATION_X   : write16ToEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 0*2, val); break;
		case ACCEL_MAX_ACCELERATION_Y   : write16ToEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 1*2, val); break;
		case ACCEL_MAX_ACCELERATION_Z   : write16ToEEPROM(JettyMBEEPROM.MAX_ACCELERATION_AXIS + 2*2, val); break;
		case ACCEL_MAX_EXTRUDER_NORM    : write16ToEEPROM(JettyMBEEPROM.MAX_ACCELERATION_NORMAL_MOVE, val); break;
		case ACCEL_MAX_EXTRUDER_RETRACT : write16ToEEPROM(JettyMBEEPROM.MAX_ACCELERATION_EXTRUDER_MOVE, val); break;
		case ACCEL_MAX_SPEED_CHANGE_A   : write16ToEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 3*2, val); break;
		case ACCEL_MAX_SPEED_CHANGE_B   : write16ToEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 4*2, val); break;
		case ACCEL_MAX_SPEED_CHANGE_X   : write16ToEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 0*2, val); break;
		case ACCEL_MAX_SPEED_CHANGE_Y   : write16ToEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 1*2, val); break;
		case ACCEL_MAX_SPEED_CHANGE_Z   : write16ToEEPROM(JettyMBEEPROM.MAX_SPEED_CHANGE + 2*2, val); break;
		case ACCEL_SLOWDOWN_FLAG        : setUInt8EEPROM(JettyMBEEPROM.SLOWDOWN_FLAG, (val != 0) ? 1 : 0); break;
		case PREHEAT_DURING_PAUSE       : setUInt8EEPROM(JettyMBEEPROM.HEAT_DURING_PAUSE, (val != 0) ? 1 : 0); break;
		case OVERRIDE_GCODE_TEMP        : setUInt8EEPROM(JettyMBEEPROM.OVERRIDE_GCODE_TEMP, (val != 0) ? 1 : 0); break;
		default : Base.logger.log(Level.WARNING, "setEEPROMParam(" + param + ", " + val + ") call failed"); break;
		}
	}

	@Override
	public void setEEPROMParam(EEPROMParams param, long val) {
		if (val < 0L)
			val = 0L;
		switch (param) {
		default : Base.logger.log(Level.WARNING, "setEEPROMParam(" + param + ", " + val + ") call failed"); break;
		}
	}

	@Override
	public void setEEPROMParam(EEPROMParams param, double val) {
		if (val < 0.0d)
			val = 0.0d;
		switch (param) {
		case ACCEL_ADVANCE_K            : setUInt32EEPROM(JettyMBEEPROM.JKN_ADVANCE_K, (long)(val * 100000.0d)); break;
		case ACCEL_ADVANCE_K2           : setUInt32EEPROM(JettyMBEEPROM.JKN_ADVANCE_K2, (long)(val * 100000.0d)); break;
		default : Base.logger.log(Level.WARNING, "setEEPROMParam(" + param + ", " + val + ") call failed"); break;
		}
	}
}


/* footnote[1]:
 MakerBot added a bunch of awesome advanced features, new commands, and new EEPROM layout for version 6
 of firmware. This firmware is shipped for several products, especially Replicator and Replicator 2. 
 For that reason, this class has some switching of functionality based on version, so Replicator can 
 get to those features.  Mostly those new features are in the Replicator2 driver class
*/ 

/* footnote[2]:
 * These functions are brought up here, since offset behavior changes for Firmware 6.0+ . TL;DR: G10, G54,
 * G55 should have no effect on firmware 6.0+, and should simply post an error
 */
