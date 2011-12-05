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
import replicatorg.drivers.OnboardParameters.EndstopType;
import replicatorg.drivers.OnboardParameters.EstopType;
import replicatorg.drivers.OnboardParameters.ExtraFeatures;
import replicatorg.drivers.gen3.PacketProcessor.CRCException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolModel;
import replicatorg.uploader.FirmwareUploader;
import replicatorg.util.Point5d;

import replicatorg.drivers.gen3.*;
import java.util.Hashtable;



class MightyBoardEEPROM implements EEPROMClass
{
	
	public static final int EEPROM_CHECK_LOW = 0x5A;
	public static final int EEPROM_CHECK_HIGH = 0x78;
		
	/// EEPROM map:
	/// 00-01 - EEPROM data version
	/// 02    - Axis inversion byte
	/// 32-47 - Machine name (max. 16 chars)
	final public static int EEPROM_CHECK_OFFSET = 0;
	final public static int EEPROM_AXIS_INVERSION_OFFSET = 2;
	final public static int EEPROM_ENDSTOP_INVERSION_OFFSET = 3;
	final public static int EEPROM_MACHINE_NAME_OFFSET = 32;
	final public static int EEPROM_AXIS_HOME_POSITIONS_OFFSET = 96;
	final public static int EEPROM_ESTOP_CONFIGURATION_OFFSET = 116;
	
	// EEPROM pot, size, offset, etc.
	final public static int EEPROM_DIGITAL_POT_OFFSET = 0x0150;
	final public static int DIGITAL_POT_COUNT = 5;
	final public static int DIGITAL_POT_BYSIZE = 1;
	
	final static class ECThermistorOffsets {
		final public static int[] TABLE_OFFSETS = {
			0x0155,
			0x01C5
		};

		final public static int R0 = 0x00;
		final public static int T0 = 0x04;
		final public static int BETA = 0x08;
		final public static int DATA = 0x10;
		
		public static int r0(int which) { return R0 + TABLE_OFFSETS[which]; }
		public static int t0(int which) { return T0 + TABLE_OFFSETS[which]; }
		public static int beta(int which) { return BETA + TABLE_OFFSETS[which]; }
		public static int data(int which) { return DATA + TABLE_OFFSETS[which]; }
	};	

	final public static int EC_EEPROM_EXTRA_FEATURES = 0x0018;
	final public static int EC_EEPROM_SLAVE_ID = 0x001A;

	final public static int MAX_MACHINE_NAME_LEN = 16;

}



/**
 * Object for managing the connection to the MightyBoard hardware.
 * @author farmckon
 */
public class MightyBoard extends Makerbot4GDriver
	implements OnboardParameters, SDCardCapture
{
	// note: other machines also inherit: PenPlotter, MultiTool
	
	/// Stores LED color by effect. Mostly uses '0' (color now)
	private Hashtable ledColorByEffect;
	
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
	}


	/**
	 *  Sets the reference voltage for the specified stepper. This will change the reference voltage
	 *  and therefore the power used by the stepper. Voltage range is 0v (0) to 1.7v (127) for Replicator machine
	 * @param stepperId target stepper index
	 * @param referenceValue the reference voltage value, from 0-127
	 * @throws RetryException
	 */
	public void setStepperVoltage(int stepperId, int referenceValue) throws RetryException {
		Base.logger.info("MightBoard sending setStepperVoltage");
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_STEPPER_REFERENCE_POT.getCode());
		pb.add8(stepperId);
		pb.add8(referenceValue); //range should be only is 0-127
		runCommand(pb.getPacket());
	}
	
	
	/**
	 * Sends a command to the 3d printer to set it's LEDs.   Sets color, and possible effect flag
	 * @param color The desired color to set the leds to
	 * @param effectId The effect for the LED to set.  NOTE: some effects do not immedately change colors, but
	 * 		store color information for later use.  Zero indicates 'set color immedately'
	 * @throws RetryException
	 */
	public void setLedStrip(Color color, int effectId) throws RetryException {
		Base.logger.info("MightBoard sending setLedStrip");
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_LED_STRIP_COLOR.getCode());
		
		pb.add8(effectId);
		pb.add8(color.getRed());
		pb.add8(color.getGreen());
		pb.add8(color.getBlue());
		PacketResponse resp =  runCommand(pb.getPacket());
		if(resp.isOK()) {
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
		Base.logger.info("MightBoard sending setBeep");
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_BEEP.getCode());
		pb.add16(frequencyHz);
		pb.add16(durationMs);
		pb.add8(effectId);		
		runCommand(pb.getPacket());
	}	
	
	

	private void checkEEPROM() {
		if (!eepromChecked) {
			// Versions 2 and up have onboard eeprom defaults and rely on 0xff values
			eepromChecked = true;
			if (version.getMajor() < 2) {
				byte versionBytes[] = readFromEEPROM(MightyBoardEEPROM.EEPROM_CHECK_OFFSET,2);
				if (versionBytes == null || versionBytes.length < 2) return;
				if ((versionBytes[0] != MightyBoardEEPROM.EEPROM_CHECK_LOW) || 
						(versionBytes[1] != MightyBoardEEPROM.EEPROM_CHECK_HIGH)) {
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
			}
		}
	}
	
	public EnumSet<AxisId> getInvertedParameters() {
		checkEEPROM();
		byte[] b = readFromEEPROM(MightyBoardEEPROM.EEPROM_AXIS_INVERSION_OFFSET,1);
		EnumSet<AxisId> r = EnumSet.noneOf(AxisId.class);
		if ( (b[0] & (0x01 << 0)) != 0 ) r.add(AxisId.X);
		if ( (b[0] & (0x01 << 1)) != 0 ) r.add(AxisId.Y);
		if ( (b[0] & (0x01 << 2)) != 0 ) r.add(AxisId.Z);
		if ( (b[0] & (0x01 << 3)) != 0 ) r.add(AxisId.A);
		if ( (b[0] & (0x01 << 4)) != 0 ) r.add(AxisId.B);
		if ( (b[0] & (0x01 << 7)) != 0 ) r.add(AxisId.V);
		return r;
	}

	public void setInvertedParameters(EnumSet<AxisId> axes) {
		byte b[] = new byte[1];
		if (axes.contains(AxisId.X)) b[0] = (byte)(b[0] | (0x01 << 0));
		if (axes.contains(AxisId.Y)) b[0] = (byte)(b[0] | (0x01 << 1));
		if (axes.contains(AxisId.Z)) b[0] = (byte)(b[0] | (0x01 << 2));
		if (axes.contains(AxisId.A)) b[0] = (byte)(b[0] | (0x01 << 3));
		if (axes.contains(AxisId.B)) b[0] = (byte)(b[0] | (0x01 << 4));
		if (axes.contains(AxisId.V)) b[0] = (byte)(b[0] | (0x01 << 7));
		writeToEEPROM(MightyBoardEEPROM.EEPROM_AXIS_INVERSION_OFFSET,b);
	}

	public String getMachineName() {
		checkEEPROM();
		byte[] data = readFromEEPROM(MightyBoardEEPROM.EEPROM_MACHINE_NAME_OFFSET,
				MightyBoardEEPROM.MAX_MACHINE_NAME_LEN);
		if (data == null) { return new String(); }
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
		machineName = new String(machineName);
		if (machineName.length() > 16) { 
			machineName = machineName.substring(0,16);
		}
		byte b[] = new byte[16];
		int idx = 0;
		for (byte sb : machineName.getBytes()) {
			b[idx++] = sb;
			if (idx == 16) break;
		}
		if (idx < 16) b[idx] = 0;
		writeToEEPROM(MightyBoardEEPROM.EEPROM_MACHINE_NAME_OFFSET,b);
	}
	

	public double getAxisHomeOffset(int axis) {
		if ((axis < 0) || (axis > 4)) {
			// TODO: handle this
			return 0;
		}
		
		checkEEPROM();
		byte[] r = readFromEEPROM(MightyBoardEEPROM.EEPROM_AXIS_HOME_POSITIONS_OFFSET + axis*4, 4);
		
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
		
		writeToEEPROM(MightyBoardEEPROM.EEPROM_AXIS_HOME_POSITIONS_OFFSET + axis*4,intToLE(offsetSteps));
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

		writeToToolEEPROM(MightyBoardEEPROM.ECThermistorOffsets.beta(which),intToLE((int)beta));
		writeToToolEEPROM(MightyBoardEEPROM.ECThermistorOffsets.r0(which),intToLE((int)r0));
		writeToToolEEPROM(MightyBoardEEPROM.ECThermistorOffsets.t0(which),intToLE((int)t0));
		writeToToolEEPROM(MightyBoardEEPROM.ECThermistorOffsets.data(which),table);
	}
	
	/**
	 * 
	 * @param which if 0 this is the extruder, if 1 it's the HBP attached to the extruder
	 */
	public int getBeta(int which, int toolIndex) {
		byte r[] = readFromToolEEPROM(MightyBoardEEPROM.ECThermistorOffsets.beta(which),4, toolIndex);
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
	public int getR0(int which, int toolIndex) {
		byte r[] = readFromToolEEPROM(MightyBoardEEPROM.ECThermistorOffsets.r0(which),4, toolIndex);
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
	public int getT0(int which, int toolIndex) {
		byte r[] = readFromToolEEPROM(MightyBoardEEPROM.ECThermistorOffsets.t0(which),4, toolIndex);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}
	
	public EndstopType getInvertedEndstops() {
		checkEEPROM();
		byte[] b = readFromEEPROM(MightyBoardEEPROM.EEPROM_ENDSTOP_INVERSION_OFFSET,1);
		return EndstopType.endstopTypeForValue(b[0]);
	}

	public void setInvertedEndstops(EndstopType endstops) {
		byte b[] = new byte[1];
		b[0] = endstops.getValue();
		writeToEEPROM(MightyBoardEEPROM.EEPROM_ENDSTOP_INVERSION_OFFSET,b);
	}

	public ExtraFeatures getExtraFeatures(int toolIndex) {
		int efdat = read16FromToolEEPROM(MightyBoardEEPROM.EC_EEPROM_EXTRA_FEATURES,0x4084, toolIndex);
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
		writeToToolEEPROM(MightyBoardEEPROM.EC_EEPROM_EXTRA_FEATURES,intToLE(efdat,2));
	}

	public EstopType getEstopConfig() {
		checkEEPROM();
		byte[] b = readFromEEPROM(MightyBoardEEPROM.EEPROM_ESTOP_CONFIGURATION_OFFSET,1);
		return EstopType.estopTypeForValue(b[0]);
	}

	public void setEstopConfig(EstopType estop) {
		byte b[] = new byte[1];
		b[0] = estop.getValue();
		writeToEEPROM(MightyBoardEEPROM.EEPROM_ESTOP_CONFIGURATION_OFFSET,b);
	}
	

	public boolean setConnectedToolIndex(int index) {
		byte[] data = new byte[1];
		data[0] = (byte) index;
		// The broadcast address has changed. The safest solution is to try both.
		writeToToolEEPROM(MightyBoardEEPROM.EC_EEPROM_SLAVE_ID, data, 255); //old firmware used 255, new fw ignores this
		writeToToolEEPROM(MightyBoardEEPROM.EC_EEPROM_SLAVE_ID, data, 127); //new firmware used 127, old fw ignores this
		return false;
	}

	protected void writeToToolEEPROM(int offset, byte[] data) {
		writeToToolEEPROM(offset, data, machine.currentTool().getIndex());
	}
	
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


	
//	/**
//	 * Overridden to not talk to the DC motor driver. This driver is reused for the stepper motor fan
//	 */
//	public void enableMotor() throws RetryException {
//		Base.logger.severe("MigtyBoard does not do DC style enable motor");
//		//machine.currentTool().enableMotor();
//	}
//	
//	/**
//	 * Overridden to not talk to the DC motor driver. This driver is reused for the stepper motor fan
//	 */
//	public void disableMotor() throws RetryException {
//		Base.logger.severe("MigtyBoard does not do DC style disable motor");
//		//machine.currentTool().disableMotor();
//	}
//	

	
	
	
	
	
}




/**
 * Singleton class until MightBoard no longer inherits from it's Sanguino3GDriver
 * parent. At that point this will become a regular part of MightBoard
 * @author farmckon
 *
// */
//private class MightyBoardEEPROM implements EEPROMClass
//{
//	
//    private static MightyBoardEEPROM _instance;
//    
//    private MightyBoardEEPROM() {  }
//
//    public static synchronized EEPROMClass getInstance() {
//            if (null == _instance) {
//                    _instance = new MightyBoardEEPROM();
//            }
//            return _instance;
//    }
//    
//	final private static int EEPROM_SIZE = 0x0200;
//	final private static int MAX_MACHINE_NAME_LEN = 16;
//
//	/// EEPROM map:
//	/// x00-x01 - EEPROM data version
//	/// x02    - Axis inversion byte
//	/// x03    - EndStop inversion byte
//	final private static int EEPROM_CHECK_OFFSET = 0;
//	final private static int EEPROM_AXIS_INVERSION_OFFSET = 2;
//	final private static int EEPROM_ENDSTOP_INVERSION_OFFSET = 3;
//	
//	/// x20-x30 - Machine name (max. 16 chars)
//	final private static int EEPROM_MACHINE_NAME_OFFSET = 32;
//	
//	/// x60		- Home position
//	final private static int EEPROM_AXIS_HOME_POSITIONS_OFFSET = 96;
//	/// x62 	- Board Features bit-set* See below
//	/// x64		- Backoff stop time
//	/// x66		- Backoff reverse time
//	/// x68		- Backoff forwrd time
//	//  x6A		- Backoff trigger time
//	
//	final private static int EEPROM_ESTOP_CONFIGURATION_OFFSET = 116;
//	
//	/// Thermister table class/objects
//	final static class ECThermistorOffsets {
//		final private static int[] TABLE_OFFSETS = {
//			0x0155,
//			0x01C5
//		};
//
//		final private static int R0 = 0x00;
//		final private static int T0 = 0x04;
//		final private static int BETA = 0x08;
//		final private static int DATA = 0x10;
//		
//		public static int r0(int which) { return R0 + TABLE_OFFSETS[which]; }
//		public static int t0(int which) { return T0 + TABLE_OFFSETS[which]; }
//		public static int beta(int which) { return BETA + TABLE_OFFSETS[which]; }
//		public static int data(int which) { return DATA + TABLE_OFFSETS[which]; }
//	};	
//
//	final static class DigitalPotentiometer
//	{
//		final private static int DIGITAL_POT_BASE = 0x0150;
//		public static int getPotOffset(int potId) {return  DIGITAL_POT_BASE + potId;}
//	}
//	
//	final private static int EC_EEPROM_EXTRA_FEATURES = 0x0018;
//	final private static int EC_EEPROM_SLAVE_ID = 0x001A;
//
//	private void checkEEPROM() {
//
//		
//		private boolean eepromChecked = false;
//		if (!eepromChecked) {
//			// Versions 2 and up have onboard eeprom defaults and rely on 0xff values
//			eepromChecked = true;
//			if (version.getMajor() < 2) {
//				byte versionBytes[] = readFromEEPROM(EEPROM_CHECK_OFFSET,2);
//				if (versionBytes == null || versionBytes.length < 2) return;
//				if ((versionBytes[0] != EEPROM_CHECK_LOW) || 
//						(versionBytes[1] != EEPROM_CHECK_HIGH)) {
//					Base.logger.severe("Cleaning EEPROM to v1.X state");
//					// Wipe EEPROM
//					byte eepromWipe[] = new byte[16];
//					Arrays.fill(eepromWipe,(byte)0x00);
//					eepromWipe[0] = EEPROM_CHECK_LOW;
//					eepromWipe[1] = EEPROM_CHECK_HIGH;
//					writeToEEPROM(0,eepromWipe);
//					Arrays.fill(eepromWipe,(byte)0x00);
//					for (int i = 16; i < 256; i+=16) {
//						writeToEEPROM(i,eepromWipe);
//					}
//				}
//			}
//		}
//	}
//
//
//}

