/*
 Makerbot4GSailfish.java

 This is a driver to control a machine that uses 4th Generation Stepstuder based electronics.

 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

 Updated for Sailfish
 Copyright (c) 17th September 2012 Jetty / Dan Newman

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

import replicatorg.app.Base;
import replicatorg.drivers.InteractiveDisplay;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.Version;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolheadsOffset;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;

/**
 * Object for managing the connection to the Makerbot 4G hardware.
 * @author farmckon
 */
public class Makerbot4GSailfish extends Makerbot4GAlternateDriver
        implements InteractiveDisplay
	//implements OnboardParameters, SDCardCapture
{
	// note: other machines also inherit: PenPlotter, MultiTool
	
	/// Stores LED color by effect. Mostly uses '0' (color now)
	private Hashtable ledColorByEffect;

	private boolean eepromChecked = false;
	
	protected final static int DEFAULT_RETRIES = 5;
	
 	protected VidPid machineId = VidPid.UNKNOWN;

	private int toolCountOnboard = -1; /// no count aka FFFF

	Version toolVersion = new Version(0,0);
	Version accelerationVersion = new Version(0,0);

	/** 
	 * Standard Constructor
	 */
	public Makerbot4GSailfish() {
		super();
		ledColorByEffect = new Hashtable();
		ledColorByEffect.put(0, Color.BLACK);
		Base.logger.info("Created a Sailfish");

		// Make sure this accurately reflects the minimum preferred
		// firmware version we want this driver to support.
		minimumVersion = new Version(4,0);
		preferredVersion = new Version(4,0);
		minimumAccelerationVersion = new Version(4,0);
		minimumJettyAccelerationVersion = new Version(4,0);
	}

	public String getDriverName() {
		return "Makerbot4GSailfish";
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

		getMotorRPM();		//load our motor RPM from firmware if we can.
		getAccelerationState();

		if (verifyMachineId() == false ) //read and verify our PID/VID if we can
		{
			Base.logger.severe("Machine ID Mismatch. Please re-select your machine.");
			return true;//TEST just for now, due to EEPROM munging
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
		boolean needsReset = checkAndWriteStepsPerMM();
		needsReset |= checkAndWriteAxisLengths();
		needsReset |= checkAndWriteMaxFeedRates();
		if ( needsReset ) {
               		setInitialized(true);	//Needed to get a proper reset
			reset();
               		setInitialized(false);
		}

		return true;
	}
	

        /**
         * Check out EEPROM settings for correctness
	 * Copied from Sanguino3GDriver.java so to not risk interfering with definition in MightyBoard.java
         */
        private void checkEEPROM() {
                if (!eepromChecked) {
                        // Versions 2 and up have onboard eeprom defaults and rely on 0xff
                        // values
                        eepromChecked = true;
                        if (version.getMajor() < 2) {
                                byte versionBytes[] = readFromEEPROM(
                                                SailfishEEPROM.EEPROM_CHECK_OFFSET, 2);
                                if (versionBytes == null || versionBytes.length < 2)
                                        return;
                                if ((versionBytes[0] != SailfishEEPROM.EEPROM_CHECK_LOW)
                                                || (versionBytes[1] != SailfishEEPROM.EEPROM_CHECK_HIGH)) {
                                        Base.logger.severe("Cleaning EEPROM to v1.X state");
                                        // Wipe EEPROM
                                        byte eepromWipe[] = new byte[16];
                                        Arrays.fill(eepromWipe, (byte) 0x00);
                                        eepromWipe[0] = SailfishEEPROM.EEPROM_CHECK_LOW;
                                        eepromWipe[1] = SailfishEEPROM.EEPROM_CHECK_HIGH;
                                        writeToEEPROM(0, eepromWipe);
                                        Arrays.fill(eepromWipe, (byte) 0x00);
                                        for (int i = 16; i < 256; i += 16) {
                                                writeToEEPROM(i, eepromWipe);
                                        }
                                }
                        }
                }
        }

        
	/// Read acceleration OFF/ON status from Bot
	private void getAccelerationState(){
	    
	    Base.logger.fine("Geting Acceleration Status from Bot");
	    acceleratedFirmware = getAccelerationStatus() != 0;
	    if(acceleratedFirmware)
	        Base.logger.finest("Found accelerated firmware active");
	    
	}

	
	// Checks the steps per mm stored in the firmware for all axis, and updates them to
	// match the ones stored in the machine xml if they are different

	public boolean checkAndWriteStepsPerMM() {

		if (!hasJettyAcceleration())
			return false;

		Point5d machineStepsPerMM = getMachine().getStepsPerMM();

		boolean needsReset = false;
		int stepperCount = 5;
		for(int i = 0; i < stepperCount; i++) {
			double firmwareAxisStepsPerMM = read64FromEEPROM(SailfishEEPROM.STEPS_PER_MM_X + i*8);

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
	
			val = (long)(val * 10000000000L);

			if ( firmwareAxisStepsPerMM != val ) {
				Base.logger.info("Bot StepsPerMM Axis " + i + ": " + firmwareAxisStepsPerMM / 10000000000.0 + 
						 " machine xml has: " + val / 10000000000.0+ ", updating bot");
				write64ToEEPROM64(SailfishEEPROM.STEPS_PER_MM_X + i*8, (long)val);
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
		int stepperCount = 5;
		for(int i = 0; i < stepperCount; i++) {
			double firmwareAxisMaximumFeedRate = read32FromEEPROM(SailfishEEPROM.ACCEL_MAX_FEEDRATE_X + i*4);

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
				write32ToEEPROM32(SailfishEEPROM.ACCEL_MAX_FEEDRATE_X + i*4, (int)val);
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
		int stepperCount = 5;
		for(int i = 0; i < stepperCount; i++) {
			int firmwareAxisLength = read32FromEEPROM(SailfishEEPROM.AXIS_LENGTHS + i*4);

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
				write32ToEEPROM32(SailfishEEPROM.AXIS_LENGTHS + i*4, val);
				needsReset = true;
			}
		}

		return needsReset;
	}

	@Override
	public void queuePoint(final Point5d p) throws RetryException {
		// If we don't know our current position, make this move an old-style move.
		// Typically we need this after home offsets are recalled otherwise an axis
		// can overspeed or underspeed due to the current position not being known
		// as we're not connected to a bot, so we can't query it.
		// Because we don't know our position, we can't calculate the feedrate
		// or distance correctly, so we demote to a non accelerated command with a
		// fixed dda calculated by Makerbot4GAlternateDriver.java.
		if (positionLost()) {
			//System.out.println(p.toString());
			//System.out.println("Position Lost");
			super.queuePoint(p);
			return;
		}

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
		//System.out.println("From: " + current.toString() + " To: " + target.toString());
		
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
		
			Point5d deltaMMAbs = new Point5d(deltaMM);
			deltaMMAbs.absolute();
			double feedrate = getSafeFeedrate(deltaMMAbs);	//Feedrate in mm/min
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
			//System.out.println("\t usec: " + usec + " dda_interval: " + dda_interval + " absolute_maximum: " + deltaSteps.absolute_maximum());
			//System.out.println("\t deltaSteps: " + deltaStepsFinal.toString() + " distance: " + distance + " feedrate: " + feedrate);
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
		Base.logger.fine("Sailfish sending setLedStrip");

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
			Base.logger.fine("Sailfish setLedStrip went OK");
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
		Base.logger.fine("Sailfish sending setBeep" + frequencyHz + durationMs + " effect" + effectId);
		Base.logger.fine("max " + Integer.MAX_VALUE);
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_BEEP.getCode());
		pb.add16(frequencyHz);
		pb.add16(durationMs);
		pb.add8(effectId);		
		PacketResponse resp =  runCommand(pb.getPacket());
		if(resp.isOK()) {
			Base.logger.fine("Sailfish sendBeep went OK");
			//beepByEffect.put(effectId, color);	
		}

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

	@Override
	public boolean hasToolheadsOffset() {
		if (machine.getTools().size() == 1)	return false;
		return true;
	}

	@Override
	public double getToolheadsOffset(int axis) {

		Base.logger.finest("Sailfish getToolheadsOffset" + axis);
		if ((axis < 0) || (axis > 2)) {
			// TODO: handle this
			Base.logger.severe("axis out of range" + axis);
			return 0;
		}
		
		checkEEPROM();

		double val = read32FromEEPROM(SailfishEEPROM.TOOLHEAD_OFFSET_SETTINGS + axis*4);

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
		//Base.logger.severe("Sailfish fetching from getConfig");
		if( this.getAccelerationStatus() != 0 ) {
			//Base.logger.severe("Sailfish is accel");
			if ( key.equals("desiredFeedrate")  )  return "80";
			if ( key.equals("travelFeedrate") )    return "150";
			if ( key.equals("printTemp") )    		return "240";
			
		} else  {
			//Base.logger.severe("Sailfish is not accel");
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
		write32ToEEPROM32(SailfishEEPROM.TOOLHEAD_OFFSET_SETTINGS + axis*4,offsetSteps);
	}
        
        @Override
        // get stored acceleration status:
	//    bit 0:  OFF (0) or ON (1)
        public byte getAccelerationStatus(){
                
                Base.logger.finest("Sailfish getAccelerationStatus");
            
                checkEEPROM();

		if (hasJettyAcceleration()) {
			byte[] val;
			val = readFromEEPROM(SailfishEEPROM.STEPPER_DRIVER, 1);
			return val[0];
		}

		return 0;
        }

        @Override
        // set stored acceleration status: either ON of OFF
        // acceleration is applied to all moves, except homing when ON
        public void setAccelerationStatus(byte status){
            Base.logger.info("Sailfish setAccelerationStatus");
            
            byte b[] = new byte[1];
            b[0] = status;

	    if (hasJettyAcceleration())
		    writeToEEPROM(SailfishEEPROM.STEPPER_DRIVER, b);
        }

	// Unhandled:  FILAMENT_USED
        
	/// Function to grab cached count of tools
	@Override
	public int toolCountOnboard() { return toolCountOnboard; } 

	
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
                return this.machineId.equals(VidPid.SAILFISH_G34);
        }

        @Override
        public boolean canVerifyMachine() {
                return true;
        }

        /// Check the EEPROM to see what PID/VID the machine believes it has
        public void readMachineVidPid() {
                checkEEPROM();
                byte[] b = readFromEEPROM(SailfishEEPROM.VID_PID_INFO,4);

                this.machineId = VidPid.getPidVid(b);
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

	/// read a 64 bit long from EEPROM at location 'offset'
	private long read64FromEEPROM(int offset)
	{
		long val = 0;
		byte[] r = readFromEEPROM(offset, 8);
		if( r == null || r.length < 8) {
			Base.logger.severe("invalid read from read64FromEEPROM at "+ offset);
			return val;
		}
		for (int i = 0; i < 8; i++)
			val = val + (((long)r[i] & 0xff) << 8*i);
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

	private void write64ToEEPROM64(int offset, long value ) {
		long s = value;
		byte buf[] = new byte[8];
		for (int i = 0; i < 8; i++) {
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
	
	// Display a message on the user interface
	public void displayMessage(double seconds, String message, boolean buttonWait) throws RetryException {
		byte options = 0; //bit 1 true cause the buffer to clear, bit 2 true indicates message complete
		final int MAX_MSG_PER_PACKET = 16;
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

	/// Returns the number of tools as saved on the machine (not as per XML count)
	//@Override 
	public void readToolheadCount() { 

		byte[] toolCountByte = readFromEEPROM(SailfishEEPROM.TOOL_COUNT, 1) ;
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
		writeToEEPROM(SailfishEEPROM.TOOL_COUNT,b);
		
	}; 

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

	@Override
	public long getEEPROMParamUInt(EEPROMParams param) {
		switch (param) {
		case ACCEL_MAX_ACCELERATION_B   : return getUInt32EEPROM(SailfishEEPROM.ACCEL_MAX_ACCELERATION_B);
		case ACCEL_EXTRUDER_DEPRIME_A   : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_EXTRUDER_DEPRIME);
		case ACCEL_EXTRUDER_DEPRIME_B   : return getUInt32EEPROM(SailfishEEPROM.ACCEL_EXTRUDER_DEPRIME_B);
		default				: return super.getEEPROMParamUInt(param);
		}
	}

        // There's no useful equivalent in Java so we promote these to Int

        @Override
        public int getEEPROMParamInt(EEPROMParams param) {
                switch (param) {
                case ACCEL_SLOWDOWN_FLAG        : return getUInt8EEPROM(SailfishEEPROM.SLOWDOWN_FLAG);
                case DITTO_PRINT_ENABLED        : return getUInt8EEPROM(SailfishEEPROM.DITTO_PRINT_ENABLED);
		default				: return super.getEEPROMParamInt(param);
                }
        }

	@Override
	public double getEEPROMParamFloat(EEPROMParams param) {
		switch (param) {
		case ACCEL_MAX_SPEED_CHANGE_B   : return (double)getUInt32EEPROM(SailfishEEPROM.ACCEL_MAX_SPEED_CHANGE_B) / 10.0d;
		default				: return super.getEEPROMParamFloat(param);
		}
	}

	@Override
	public void setEEPROMParam(EEPROMParams param, int val) {
		if (val < 0)
			val = 0;
		switch (param) {
                case ACCEL_SLOWDOWN_FLAG        : setUInt8EEPROM(SailfishEEPROM.SLOWDOWN_FLAG, (val != 0) ? 1 : 0); break;
                case DITTO_PRINT_ENABLED        : setUInt8EEPROM(SailfishEEPROM.DITTO_PRINT_ENABLED, (val != 0) ? 1 : 0); break;
		default				: super.setEEPROMParam(param, val); break;
		}
	}

	@Override
	public void setEEPROMParam(EEPROMParams param, long val) {
		if (val < 0L)
			val = 0L;
		switch (param) {
		case ACCEL_MAX_ACCELERATION_B   : setUInt32EEPROM(SailfishEEPROM.ACCEL_MAX_ACCELERATION_B, val); break;
		case ACCEL_EXTRUDER_DEPRIME_A   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_EXTRUDER_DEPRIME,    val); break;
		case ACCEL_EXTRUDER_DEPRIME_B   : setUInt32EEPROM(SailfishEEPROM.ACCEL_EXTRUDER_DEPRIME_B, val); break;
		default				: super.setEEPROMParam(param, val); break;
		}
	}

	@Override
	public void setEEPROMParam(EEPROMParams param, double val) {
		if (val < 0.0d)
			val = 0.0d;
		switch (param) {
		case ACCEL_MAX_SPEED_CHANGE_B   : setUInt32EEPROM(SailfishEEPROM.ACCEL_MAX_SPEED_CHANGE_B, (long)(val * 10.0d)); break;
		default				: super.setEEPROMParam(param, val); break;
		}
	}
}
