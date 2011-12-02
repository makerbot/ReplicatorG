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
import replicatorg.drivers.gen3.PacketProcessor.CRCException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolModel;
import replicatorg.uploader.FirmwareUploader;
import replicatorg.util.Point5d;

import replicatorg.drivers.gen3.*;
import java.util.Hashtable;
/**
 * Object for managing the connection to the MightyBoard hardware.
 * @author farmckon
 */
public class MightyBoard extends Makerbot4GAlternateDriver
	implements OnboardParameters, SDCardCapture
{
	// note: other machines also inherit: PenPlotter, MultiTool
	
	/// Stores LED color by effect. Mostly uses '0' (color now)
	private Hashtable ledColorByEffect;
	
	/// 0 -127, current reference value. Store on desktop for this machine
	private int voltageReference; 

	protected final static int DEFAULT_RETRIES = 5;
	
	Version toolVersion = new Version(0,0);

	/** 
	 * Standard Constructor
	 */
	public MightyBoard() {
		super();
		ledColorByEffect = new Hashtable();
		ledColorByEffect.put(0, Color.BLACK);
		Base.logger.fine("Created a MightBoard");
	}


	/**
	 *  Sets the reference voltage for the specified stepper. This will change the reference voltage
	 *  and therefore the power used by the stepper. Voltage range is 0v (0) to 1.7v (127) for Replicator machine
	 * @param stepperId target stepper index
	 * @param referenceValue the reference voltage value, from 0-127
	 * @throws RetryException
	 */
	public void setStepperVoltage(int stepperId, int referenceValue) throws RetryException {
		Base.logger.fine("MightBoard sending setStepperVoltage");
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
		Base.logger.fine("MightBoard sending setLedStrip");
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_LED_STRIP_COLOR.getCode());
		pb.add8(effectId);
		pb.add8(color.getRed());
		pb.add8(color.getGreen());
		pb.add8(color.getBlue());
		runCommand(pb.getPacket());
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
		Base.logger.fine("MightBoard sending setBeep");
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_BEEP.getCode());
		pb.add16(frequencyHz);
		pb.add16(durationMs);
		pb.add8(effectId);		
		runCommand(pb.getPacket());
	}	

}
