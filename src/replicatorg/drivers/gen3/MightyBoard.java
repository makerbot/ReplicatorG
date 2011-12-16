/*
 MightyBoard.java

 This is a driver to control a machine that uses the MightBoard electronics.

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

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import java.awt.Color;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.drivers.DriverError;
import replicatorg.drivers.MultiTool;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.PenPlotter;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SDCardCapture;
import replicatorg.drivers.SerialDriver;
import replicatorg.drivers.Version;
import replicatorg.drivers.OnboardParameters.BackoffParameters;
import replicatorg.drivers.OnboardParameters.EndstopType;
import replicatorg.drivers.OnboardParameters.EstopType;
import replicatorg.drivers.OnboardParameters.ExtraFeatures;
import replicatorg.drivers.OnboardParameters.PIDParameters;
import replicatorg.drivers.gen3.PacketProcessor.CRCException;
//import replicatorg.drivers.gen3.Sanguino3GDriver.CoolingFanOffsets;
//import replicatorg.drivers.gen3.Sanguino3GDriver.c;
//import replicatorg.drivers.gen3.Sanguino3GDriver.PIDOffsets;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolModel;
import replicatorg.uploader.FirmwareUploader;
import replicatorg.util.Point5d;

import replicatorg.drivers.gen3.*;
import java.util.Hashtable;


//enum EepromOffsets {
//	
////	final public static int EEPROM_CHECK_OFFSET = 0;
//	EEPROM_VERSION(0x0000,2,"Eeprom Version info");
//	AXIS_INVERSION(0x0002,1,
//			"Axis N (where X=0, Y=1, etc.) is inverted if the Nth bit is set." +
//			"Bit 7 is used for HoldZ OFF: 1 = off, 0 = on");
//	
//
//	public final int offset;
//	public final String info;
//	
//	/// standard constructor. 
//	private EepromOffsets(int offset, int size, String info){
//		this.offset = offset;
//		this.byteSz = size;
//		this.info = info;
//	}
//}

final class PIDTermOffsets {
	final static int P_TERM_OFFSET = 0x0000;
	final static int I_TERM_OFFSET = 0x0002;
	final static int D_TERM_OFFSET = 0x0004;
};


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
}

class MightyBoardEEPROM implements EEPROMClass
{
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
	final public static int ENDSTOP_INVERSION			= 0x0003;
	/// Name of this machine: 32 bytes.
	final public static int MACHINE_NAME				= 0x0020;
	/// Default locations for the axis: 5 x 32 bit = 20 bytes
	final public static int AXIS_HOME_POSITIONS		= 0x0060;
	/// Thermistor table 0: 128 bytes
	final public static int THERM_TABLE		= 0x0074;
	/// Padding: 8 bytes
	// Toolhead 0 data: 26 bytes (see above)
	final public static int T0_DATA_BASE		= 0x100;
	// Toolhead 0 data: 26 bytes (see above)
	final public static int T1_DATA_BASE		= 0x011C;
	/// Digital Potentiometer Settings : 5 Bytes
	final public static int DIGI_POT_SETTINGS			= 0x0138;
	/// hardare version id
	final public static int HARDWARE_ID 				= 0x013D;
	/// Ligth Effect table. 3 Bytes x 3 entries
	final public static int LED_STRIP_SETTINGS		= 0x013E;
	/// Buzz Effect table. 4 Bytes x 3 entries
	final public static int BUZZ_SETTINGS		= 0x0147;

	/// start of free space
	final public static int FREE_EEPROM_STARTS = 0x0153;

	// tag for Mightyboard V1 shipping hardware
	final public static int HARDWARE_ID_LMIGHTYBOARD_A = 0x1213;

}


/**
 * Object for managing the connection to the MightyBoard hardware.
 * @author farmckon
 */
public class MightyBoard extends Makerbot4GAlternateDriver
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
	
	Version toolVersion = new Version(0,0);

	/** 
	 * Standard Constructor
	 */
	public MightyBoard() {
		super();
		ledColorByEffect = new Hashtable();
		ledColorByEffect.put(0, Color.BLACK);
		Base.logger.info("Created a MightBoard");

		stepperValues= new Hashtable();

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

		int stepperCountMightyBoard = 5;
		Base.logger.severe("MightBoard initial Sync");
		for(int i = 0; i < stepperCountMightyBoard; i++)
		{
			int vRef = getStoredStepperVoltage(i); 
			Base.logger.info("storing inital stepper Values from onboard eeprom");
			Base.logger.info("i = " + i + " vRef =" + vRef);
			stepperValues.put(new Integer(i), new Integer(vRef) );
		}
		
		//load our motor RPM from firmware if we can.
		getMotorRPM();

		//
		getSpindleSpeedPWM();
		return true;
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
		Base.logger.severe("MightBoard sending setStepperVoltage: " + stepperId + " " + referenceValue);
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

	
	@Override
	public boolean hasVrefSupport() {
		return true;
	}
	
	
	
	@Override
	public int getStoredStepperVoltage(int stepperId) 
	{
		Base.logger.severe("Getting stored stepperVoltage: " + stepperId );
		int vRefForPotLocation = MightyBoardEEPROM.DIGI_POT_SETTINGS + stepperId;
		
		Base.logger.severe("Getting stored stepperVoltage from eeprom addr: " +
				vRefForPotLocation  );

		byte[] voltages = readFromEEPROM(vRefForPotLocation, 1) ;
		if(voltages == null ) {
			Base.logger.severe("null response to EEPROM read");
			return 0;
		}
		Base.logger.severe("raw stored stepperVoltage: " + voltages[0]);

		if(voltages[0] > 127)		voltages[0] = 127;
		else if(voltages[0] < 0)	voltages[0] = 0;

		Base.logger.severe("Effective stored stepperVoltage: " + voltages[0]);
		return (int)voltages[0];
		
	}


	@Override
	public void setStoredStepperVoltage(int stepperId, int referenceValue) {
		Base.logger.info("MightBoard sending storeStepperVoltage");

		if(stepperId > 5) {
			Base.logger.severe("store invalid stepper Id" + Integer.toString(stepperId) );
			return; 
		}
		if (referenceValue > 127)		referenceValue= 127; 
		else if (referenceValue < 0)	referenceValue= 0; 

		int vRefForPotLocation = MightyBoardEEPROM.DIGI_POT_SETTINGS + stepperId;
		byte b[] = new byte[1];
		b[0] =  (byte)referenceValue;
		checkEEPROM();
		writeToEEPROM(vRefForPotLocation, b);
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
		Base.logger.severe("MightBoard sending setLedStrip");
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_LED_STRIP_COLOR.getCode());

                int Channel = 1;
                int Brightness = 60;
                int BlinkRate = 10;
                int LEDs = 0x33;
		pb.add8(Channel);//color.getRed());
		pb.add8(Brightness);//color.getGreen());
		pb.add8(BlinkRate);//color.getBlue());
                pb.add8(LEDs);
		pb.add8(effectId);

		PacketResponse resp =  runCommand(pb.getPacket());
		if(resp.isOK()) {
			Base.logger.severe("MightBoard setLedStrip went OK");
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
		Base.logger.severe("MightBoard sending setBeep" + frequencyHz + durationMs + " effect" + effectId);
		Base.logger.severe("max " + Integer.MAX_VALUE);
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_BEEP.getCode());
		pb.add16(frequencyHz);
		pb.add16(durationMs);
		pb.add8(effectId);		
		PacketResponse resp =  runCommand(pb.getPacket());
		if(resp.isOK()) {
			Base.logger.severe("MightBoard sendBeep went OK");
			//beepByEffect.put(effectId, color);	
		}

	}	
	
	
	private void checkEEPROM() {
		if (!eepromChecked) {
			// Versions 2 and up have onboard eeprom defaults and rely on 0xff values
			eepromChecked = true;
			if (version.getMajor() < 2) {
				byte versionBytes[] = readFromEEPROM(MightyBoardEEPROM.VERSION_LOW,2);
				if (versionBytes == null || versionBytes.length < 2) 
					return;
				if ((versionBytes[0] != MightyBoardEEPROM.EEPROM_CHECK_LOW) || 
					(versionBytes[1] != MightyBoardEEPROM.EEPROM_CHECK_HIGH)) 
				{
					Base.logger.severe("Cleaning EEPROM to v1.X state");
					// Wipe EEPROM
					byte eepromWipe[] = new byte[16];
					Arrays.fill(eepromWipe,(byte)0x00);
					eepromWipe[0] = MightyBoardEEPROM.EEPROM_CHECK_LOW;
					eepromWipe[1] = MightyBoardEEPROM.EEPROM_CHECK_HIGH;
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
	public EnumSet<AxisId> getInvertedParameters() {
		checkEEPROM();
		byte[] b = readFromEEPROM(MightyBoardEEPROM.AXIS_INVERSION,1);
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
	public void setInvertedParameters(EnumSet<AxisId> axes) {
		byte b[] = new byte[1];
		if (axes.contains(AxisId.X)) b[0] = (byte)(b[0] | (0x01 << 0));
		if (axes.contains(AxisId.Y)) b[0] = (byte)(b[0] | (0x01 << 1));
		if (axes.contains(AxisId.Z)) b[0] = (byte)(b[0] | (0x01 << 2));
		if (axes.contains(AxisId.A)) b[0] = (byte)(b[0] | (0x01 << 3));
		if (axes.contains(AxisId.B)) b[0] = (byte)(b[0] | (0x01 << 4));
		if (axes.contains(AxisId.V)) b[0] = (byte)(b[0] | (0x01 << 7));
		writeToEEPROM(MightyBoardEEPROM.AXIS_INVERSION,b);
	}

	@Override
	public String getMachineName() {
		checkEEPROM();
		
		byte[] data = readFromEEPROM(MightyBoardEEPROM.MACHINE_NAME,
				MightyBoardEEPROM.MAX_MACHINE_NAME_LEN);
		Base.logger.severe("getting getMachineName");

		if (data == null) { return "no name"; }
		try {
			int len = 0;
			while (len < MightyBoardEEPROM.MAX_MACHINE_NAME_LEN && data[len] != 0) len++;
			return new String(data,0,len,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}


	public void setMachineName(String machineName) {
		int maxLen = MightyBoardEEPROM.MAX_MACHINE_NAME_LEN;
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
		writeToEEPROM(MightyBoardEEPROM.MACHINE_NAME,b);
	}
	

	@Override
	public double getAxisHomeOffset(int axis) {

		Base.logger.severe("MigtyBoard getAxisHomeOffset" + axis);
		if ((axis < 0) || (axis > 4)) {
			// TODO: handle this
			Base.logger.severe("axis out of range" + axis);
			return 0;
		}
		
		checkEEPROM();
		byte[] r = readFromEEPROM(MightyBoardEEPROM.AXIS_HOME_POSITIONS + axis*4, 4);
		if( r == null || r.length < 4) {
			Base.logger.severe("invalid read from AXIS_HOME_POSITION");
			return 0; }

		double val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
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
		
		writeToEEPROM(MightyBoardEEPROM.AXIS_HOME_POSITIONS + axis*4,intToLE(offsetSteps));
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
		eepromIndicator[0] = MightyBoardEEPROM.EEPROM_CHECK_LOW;
		eepromIndicator[1] = MightyBoardEEPROM.EEPROM_CHECK_HIGH;
		writeToToolEEPROM(0,eepromIndicator);

		writeToEEPROM(MightyBoardEEPROM.ECThermistorOffsets.beta(which),intToLE((int)beta));
		writeToEEPROM(MightyBoardEEPROM.ECThermistorOffsets.r0(which),intToLE((int)r0));
		writeToEEPROM(MightyBoardEEPROM.ECThermistorOffsets.t0(which),intToLE((int)t0));
		writeToEEPROM(MightyBoardEEPROM.ECThermistorOffsets.data(which),table);
	}
	
	/**
	 * 
	 * @param which if 0 this is the extruder, if 1 it's the HBP attached to the extruder
	 */
	@Override
	public int getBeta(int which, int toolIndex) {
		Base.logger.severe("beta for " + Integer.toString(toolIndex));
		byte r[] = readFromEEPROM(MightyBoardEEPROM.ECThermistorOffsets.beta(which),4);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}
	
	@Override
	public EndstopType getInvertedEndstops() {
		checkEEPROM();
		byte[] b = readFromEEPROM(MightyBoardEEPROM.ENDSTOP_INVERSION,1);
		return EndstopType.endstopTypeForValue(b[0]);
	}

	@Override
	public void setInvertedEndstops(EndstopType endstops) {
		byte b[] = new byte[1];
		b[0] = endstops.getValue();
		writeToEEPROM(MightyBoardEEPROM.ENDSTOP_INVERSION,b);
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
		byte[] b = readFromEEPROM(MightyBoardEEPROM.ENDSTOP_INVERSION,1);
		return EstopType.estopTypeForValue(b[0]);
	}

	@Override
	public void setEstopConfig(EstopType estop) {
		byte b[] = new byte[1];
		b[0] = estop.getValue();
		writeToEEPROM(MightyBoardEEPROM.ENDSTOP_INVERSION,b);
	}
	

	@Override
	public boolean setConnectedToolIndex(int index) {
		byte[] data = new byte[1];
		data[0] = (byte) index;
		Base.logger.severe("setConnectedToolIndex not supported in MightyBoard");
		
		//throw new UnsupportedOperationException("setConnectedToolIndex not supported in MightyBoard");
		
		// The broadcast address has changed. The safest solution is to try both.
		//writeToToolEEPROM(MightyBoardEEPROM.EC_EEPROM_SLAVE_ID, data, 255); //old firmware used 255, new fw ignores this
		//writeToToolEEPROM(MightyBoardEEPROM.EC_EEPROM_SLAVE_ID, data, 127); //new firmware used 127, old fw ignores this
		return false;
	}

	@Override
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
	protected byte[] readFromToolEEPROM(int offset, int len, int toolIndex) {


		int toolInfoOffset = 0;
		if (toolIndex == 0)	toolInfoOffset = MightyBoardEEPROM.T0_DATA_BASE;
		else if (toolIndex == 1)toolInfoOffset = MightyBoardEEPROM.T1_DATA_BASE;

		offset = toolInfoOffset + offset;
		Base.logger.severe("readFromToolEEPROM null" + offset +" " + len + " " + toolIndex);
				
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.READ_EEPROM.getCode());
		pb.add16(offset);
		pb.add8(len);
		PacketResponse pr = runQuery(pb.getPacket());
		if (pr.isOK()) {
			Base.logger.severe("readFromToolEEPROM ok at: " + offset +" len:" + len + " id:" + toolIndex);			
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
	public void enableMotor() throws RetryException {

		ToolModel curTool = machine.currentTool();//WARNING: this in unsafe, since tool is checked
		//async from when command is set. Tool should be a param
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
	public void disableMotor() throws RetryException {
		ToolModel curTool = machine.currentTool();//WARNING: this in unsafe, since tool is checked
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
		byte r[] = readFromEEPROM(MightyBoardEEPROM.ECThermistorOffsets.r0(which),4);
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
		byte r[] = readFromEEPROM(MightyBoardEEPROM.ECThermistorOffsets.t0(which),4);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}

	@Override
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
	 */
	@Override
	public PIDParameters getPIDParameters(int which, int toolIndex) {
		PIDParameters pp = new PIDParameters();

		int offset = (which == OnboardParameters.EXTRUDER)?
				ToolheadEEPROM.EXTRUDER_PID_BASE:ToolheadEEPROM.HBP_PID_BASE;
		if (which == OnboardParameters.EXTRUDER)
			Base.logger.severe("** PID FOR ID: Extruder" );
		else
			Base.logger.severe("** PID FOR ID: BuildPlatform" );


		Base.logger.severe("pid p: " + Integer.toString(toolIndex));
		pp.p = readFloat16FromToolEEPROM(offset + PIDTermOffsets.P_TERM_OFFSET, 7.0f, toolIndex);
		Base.logger.severe("pid i: " + Integer.toString(toolIndex));
		pp.i = readFloat16FromToolEEPROM(offset + PIDTermOffsets.I_TERM_OFFSET, 0.325f, toolIndex);
		Base.logger.severe("pid d: " + Integer.toString(toolIndex));
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

		Base.logger.severe("val " + (((int) r[0]) & 0xff)+ " & " + (((int) r[1]) & 0xff) );
		if (r[0] == (byte) 0xff && r[1] == (byte) 0xff){
			Base.logger.fine("ERROR: Eeprom  float 16 val at "+ offset +" is 0xFFFF");
			return defaultValue;
		}
		return (float) byteToInt(r[0]) + ((float) byteToInt(r[1])) / 256.0f;
	}

	private int byteToInt(byte b) {
		return ((int) b) & 0xff;
	}
	
	/**
	 * Reset to the factory state. This ordinarily means writing 0xff over the
	 * entire eeprom.
	 */
	@Override
	//TODO: better solution to not wiping eeprom on Mightyboard
	public void resetToFactory() {
		// reset to factory disabled for Mightyboard.
		// we do not want to overwrite home axis positions
		
		/*byte eepromWipe[] = new byte[16];
		Arrays.fill(eepromWipe, (byte) 0xff);
		for (int i = 0; i < 0x0200; i += 16) {
			writeToEEPROM(i, eepromWipe);
		}
		*/
	}

	@Override
	public void resetToolToFactory(int toolIndex) {
		byte eepromWipe[] = new byte[16];
		Arrays.fill(eepromWipe,(byte)0xff);
		for (int i = 0; i < 0x0200; i+=16) {
			writeToToolEEPROM(i,eepromWipe,toolIndex);
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
	public double getPlatformTemperatureSetting() {
		// This call was introduced in version 2.3
		if (toolVersion.atLeast(new Version(2, 3))) {
			PacketBuilder pb = new PacketBuilder(
					MotherboardCommandCode.TOOL_QUERY.getCode());
			pb.add8((byte) machine.currentTool().getIndex());
			pb.add8(ToolCommandCode.GET_PLATFORM_SP.getCode());
			PacketResponse pr = runQuery(pb.getPacket());
			int sp = pr.get16();
			machine.currentTool().setPlatformTargetTemperature(sp);
		}
		return super.getPlatformTemperatureSetting();
	}
	
	@Override
	public double getTemperatureSetting() {
		// This call was introduced in version 2.3
		if (toolVersion.atLeast(new Version(2, 3))) {
			PacketBuilder pb = new PacketBuilder(
					MotherboardCommandCode.TOOL_QUERY.getCode());
			pb.add8((byte) machine.currentTool().getIndex());
			pb.add8(ToolCommandCode.GET_SP.getCode());
			PacketResponse pr = runQuery(pb.getPacket());
			int sp = pr.get16();
			machine.currentTool().setTargetTemperature(sp);
		}
		return super.getTemperatureSetting();
	}
	
	

	public Version getToolVersion() {
		return toolVersion;
	}
	
	@Override
	public void setMotorRPM(double rpm) throws RetryException {
	
		///TRICKY: fot The Replicator,the firmware no longer handles this command
		// it's all done on host side via 5D command translation
		
	// convert RPM into microseconds and then send.
//		long microseconds = rpm == 0 ? 0 : Math.round(60.0 * 1000000.0 / rpm); // no
//		// unsigned
//		// ints?!?
//		// microseconds = Math.min(microseconds, 2^32-1); // limit to uint32.
//
//		Base.logger.fine("Setting motor 1 speed to " + rpm + " RPM ("
//				+ microseconds + " microseconds)");
//
//		// send it!
//		PacketBuilder pb = new PacketBuilder(
//				MotherboardCommandCode.TOOL_COMMAND.getCode());
//		pb.add8((byte) machine.currentTool().getIndex());
//		pb.add8(ToolCommandCode.SET_MOTOR_1_RPM.getCode());
//		pb.add8((byte) 4); // length of payload.
//		pb.add32(microseconds);
//		runCommand(pb.getPacket());
//
		
		machine.currentTool().setMotorSpeedRPM(rpm);
//		super.setMotorRPM(rpm); TODO: this should be setMotorRPM running? 
//				check read value vs running/tested value
	}

	

	
}


