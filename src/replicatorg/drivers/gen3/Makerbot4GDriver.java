package replicatorg.drivers.gen3;

import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.Arrays;

import org.w3c.dom.Element;

import replicatorg.app.Base;
import replicatorg.drivers.RetryException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.MachineModel;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;
import replicatorg.drivers.Version;

public class Makerbot4GDriver extends Sanguino3GDriver {

	private boolean accelerationEnabled = false;
	private boolean stepperExtruderFanEnabled = false;

        public Makerbot4GDriver() {
		super();
		// This will be overridden by the MightyBoard driver when it extends this class
	        minimumAccelerationVersion = new Version(3,2);
		      minimumJettyAccelerationVersion = new Version(3,2);
        }

	public String getDriverName() {
		return "Makerbot4G";
	}
	
        @Override
	public boolean hasAcceleration() { 
            if (version.compareTo(getMinimumAccelerationVersion()) < 0)
                return false;
            return true;
	}

	@Override
	public boolean hasJettyAcceleration() { 
            if (version.compareTo(getMinimumJettyAccelerationVersion()) < 0)
                return false;
            return hasAcceleration();
	}

	public void reset() {
		// We should poll the machine for it's state here, but it is more important to have the
		// fan on than off.
		stepperExtruderFanEnabled = false;
		
		super.reset();
	}

	public void stop(boolean abort) {
		// Record the toolstate as off, so we don't excite the extruder motor in future moves.
		machine.currentTool().disableMotor();

		// We should stop the fan here, but it will be stopped for us by the super.
		stepperExtruderFanEnabled = false;

		super.stop(abort);
	}
	
	private Iterable<AxisId> getHijackedAxes(int toolhead){
		Vector<AxisId> axes = new Vector<AxisId>();
		AxisId toolheadAxis = machine.getTool(toolhead).getMotorStepperAxis();
		if( extruderHijackedMap.containsKey( toolheadAxis ) )
			axes.add(toolheadAxis);
		return axes;
	}	

//	/** 
//	 * Returns the hijacked axes for the current tool.
//	 */
//	@Deprecated
//	private Iterable<AxisId> getHijackedAxes() {
//		Vector<AxisId> axes = new Vector<AxisId>();
//		
//		for ( Map.Entry<AxisId,ToolModel> entry : stepExtruderMap.entrySet()) {
//			ToolModel curTool = machine.currentTool();
//			if (curTool.equals(entry.getValue())) {
//				axes.add(curTool.getMotorStepperAxis());
//			}
//		}
//		return axes;
//	}

	/** 
	 * Returns the hijacked axes for all tools.
	 */
	private Iterable<AxisId> getAllHijackedAxes() {
		Vector<AxisId> axes = new Vector<AxisId>();
		for ( Map.Entry<AxisId,ToolModel> entry : extruderHijackedMap.entrySet()) {
			AxisId axis = entry.getKey();
			axes.add(axis);
		}
		return axes;
	}

	/** relies on currentTool too much **/
	@Deprecated 
	protected void queueAbsolutePoint(Point5d steps, long micros) throws RetryException {
		// Turn on fan if necessary
		int toolhead = machine.currentTool().getIndex();
		for (AxisId axis : getHijackedAxes(toolhead)) {
			if (steps.axis(axis) != 0) {
				enableStepperExtruderFan(true,toolhead);
			}
		}

		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.QUEUE_POINT_EXT.getCode());

		if (Base.logger.isLoggable(Level.FINE)) {
			Base.logger.log(Level.FINE,"Queued absolute point " + steps + " at "
					+ Long.toString(micros) + " usec.");
		}

		// just add them in now.
		pb.add32((int) steps.x());
		pb.add32((int) steps.y());
		pb.add32((int) steps.z());
		pb.add32((int) steps.a());
		pb.add32((int) steps.b());
		pb.add32((int) micros);

		runCommand(pb.getPacket());
	}

	public void setCurrentPosition(Point5d p) throws RetryException {
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.SET_POSITION_EXT.getCode());

		Point5d steps = machine.mmToSteps(p);
		pb.add32((long) steps.x());
		pb.add32((long) steps.y());
		pb.add32((long) steps.z());
		pb.add32((long) steps.a());
		pb.add32((long) steps.b());

		Base.logger.log(Level.FINE,"Set current position to " + p + " (" + steps
					+ ")");

		runCommand(pb.getPacket());
	
		// Set the current position explicitly instead of calling the super, to avoid sending the current position command twice.
		currentPosition.set(p);
//		super.setCurrentPosition(p);
	}

	protected Point5d reconcilePosition() {
		// If we're writing to a file, we can't actually know what the current position is.
		if (fileCaptureOstream != null) {
			return null;
		}
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.GET_POSITION_EXT.getCode());
		PacketResponse pr = runQuery(pb.getPacket());
		
		Point5d steps;
		try {
			steps = new Point5d(pr.get32(), pr.get32(), pr.get32(), pr.get32(), pr.get32());
		} catch(NullPointerException npe) {
			Base.logger.log(Level.FINEST, "Invalid response packet");
			return null;
		}
		
//		Base.logger.fine("Reconciling : "+machine.stepsToMM(steps).toString());
		return machine.stepsToMM(steps);
	}
	
	
	/**
	 * Overridden to not talk to the DC motor driver. This driver is reused for the stepper motor fan
	 */
	public void enableMotor() throws RetryException {
		Base.logger.fine("MakerBot4G.enableMotor()");//REMOVE
		machine.currentTool().enableMotor();
	}
	
	/**
	 * Overridden to not talk to the DC motor driver. This driver is reused for the stepper motor fan
	 */
	public void disableMotor() throws RetryException {
		Base.logger.fine("MakerBot4G.enableMotor()"); //REMOVE
		machine.currentTool().disableMotor();
	}
	
	/**
	 * Overridden to not talk to the DC motor driver. This driver is reused for the stepper motor fan
	 */
	public void setMotorSpeedPWM(int pwm) throws RetryException {
		machine.currentTool().setMotorSpeedPWM(pwm);
	}

	/**
	 * Overridden to not talk to the DC motor driver. This driver is reused for the stepper motor fan
	 */
	public void setMotorRPM(double rpm, int toolhead) throws RetryException {
	  if (toolhead == -1) {
		  machine.currentTool().setMotorSpeedRPM(rpm);
	  } else {
	    machine.getTool(toolhead).setMotorSpeedRPM(rpm);
	  }
	}
	
	
	public void enableDrives() throws RetryException {
		enableStepperExtruderFan(true,machine.currentTool().getIndex());
		
		super.enableDrives();
	}

	public void disableDrives() throws RetryException {
		enableStepperExtruderFan(false,machine.currentTool().getIndex());
		
		super.disableDrives();
	}
	
	/**
	 * Due to async command dispatch, this version should not be called.
	 */
	@Deprecated 
	public void enableStepperExtruderFan(boolean enabled) throws RetryException {
		enableStepperExtruderFan(enabled, machine.currentTool().getIndex());
	}

	/**
	 * Will turn on/off the stepper extruder fan if it's not already in the correct state.
	 * 
	 */
	public void enableStepperExtruderFan(boolean enabled, int toolIndex) throws RetryException {
		
		// Always re-enable the fan when 
		if (this.stepperExtruderFanEnabled == enabled) return;
		
		// FIXME: Should be called per hijacked axis with the correct tool
		// our flag variable starts with motors enabled.
		byte flags = (byte) (enabled ? 1 : 0);

		// bit 1 determines direction...
		flags |= 2;

		Base.logger.log(Level.FINE,"Stepper Extruder fan w/flags: "
					+ Integer.toBinaryString(flags));

		// send it!
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.TOOL_COMMAND.getCode());
		pb.add8((byte) toolIndex);
		pb.add8(ToolCommandCode.TOGGLE_MOTOR_1.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8(flags);
		runCommand(pb.getPacket());

		// Always use max PWM
		pb = new PacketBuilder(MotherboardCommandCode.TOOL_COMMAND.getCode());
		pb.add8((byte) toolIndex);
		pb.add8(ToolCommandCode.SET_MOTOR_1_PWM.getCode());
		pb.add8((byte) 1); // length of payload.
		pb.add8((byte) 255);
		runCommand(pb.getPacket());
		
		this.stepperExtruderFanEnabled = enabled;
	}

	EnumMap<AxisId,ToolModel> extruderHijackedMap = new EnumMap<AxisId,ToolModel>(AxisId.class);
	
	
	@Override
	/**
	 * When the machine is set for this driver, some toolheads may poach the an extrusion axis.
	 */
	public void setMachine(MachineModel m) {
		super.setMachine(m);
		for (ToolModel tm : m.getTools()) {
			Element e = (Element)tm.getXml();
			if (e.hasAttribute("stepper_axis")) {
				final String stepAxisStr = e.getAttribute("stepper_axis");
				try {
					AxisId axis = AxisId.valueOf(stepAxisStr.toUpperCase());
					if (m.hasAxis(axis)) {
						// If we're seizing an axis for an extruder, remove it from the available axes and get
						// the data associated with that axis.
						// Ted says: but we don't seem to be removing it from the available axes.
						//   We do that in the 4ga driver, but not here. 
						extruderHijackedMap.put(axis,tm);
					} else {
						Base.logger.severe("Tool claims unavailable axis "+axis.name());
					}
				} catch (IllegalArgumentException iae) {
					Base.logger.severe("Unintelligible axis designator "+stepAxisStr);
				}
			}
		}
	}
	
	
	@Override
	public EnumMap<AxisId, String> getAxisAlises() {
		/// Returns a set of Axes that are overridden or hijacked, 
		/// and a string to indicate what they are overridden or hijacked for.
		EnumMap<AxisId,String> map = new EnumMap<AxisId,String>(AxisId.class);
		for ( AxisId id : extruderHijackedMap.keySet() ) {
			ToolModel t = extruderHijackedMap.get(id);
			map.put(id,t.getName());
		}
		return map;
	}

	@Override 
	public String getMachineType(){ return "Thing-O-Matic/CupCake CNC"; } 

	/// Read acceleration OFF/ON status from Bot
	private void getAccelerationState() {
		Base.logger.fine("Geting Acceleration Status from Bot");
		accelerationEnabled = 0 != (getAccelerationStatus() & (byte)0x01);
		if (accelerationEnabled)
			Base.logger.finest("Found accelerated firmware active");
	}

	/// Looks up a key value based on the machine setting/status.
	/// Only used for getting baseline acceleration values for
	// Print-O-Matic
	@Override 
	public String getConfigValue(String key, String baseline)
	{
		//Base.logger.severe("Thing-O-Matic/CupCake CNC fetching from getConfig");
		getAccelerationState();
		if (accelerationEnabled) {
			//Base.logger.severe("Gen4 board is accel");
			if ( key.equals("desiredFeedrate")  )  return "80";
			if ( key.equals("travelFeedrate") )    return "150";
			if ( key.equals("printTemp") )         return "240";
			
		} else  {
			//Base.logger.severe("Gen4 board is not accel");
			if ( key.equals("desiredFeedrate")  )  return "40";
			if ( key.equals("travelFeedrate") )    return "55";
			if ( key.equals("printTemp") )         return "220";
		}
		return baseline;
	}

	/// read a 32 bit int from EEPROM at location 'offset'
	/// NOTE: The equivalent routine in MightyBoard.java fails for a negative-valued integer
	private int readInt32FromEEPROM(int offset)
	{
		byte[] r = readFromEEPROM(offset, 4);
		if( r == null || r.length < 4) {
			Base.logger.severe("invalid read from read32FromEEPROM at "+ offset);
			return 0;
		}
		int val = (int)r[0] & 0xff;
		val += ((int)r[1] & 0xff) <<  8;
		val += ((int)r[2] & 0xff) << 16;
		val += ((int)r[3] & 0x7f) << 24;
		if (r[3] < 0)
			val = -val;
		return val;
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

	private void writeInt32ToEEPROM(int offset, int value) {
		int s = value;
		byte buf[] = new byte[4];
		for (int i = 0; i < 4; i++) {
			buf[i] = (byte) (s & 0xff);
				s = s >>> 8;
		}
		writeToEEPROM(offset, buf);
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
		writeInt32ToEEPROM(offset, v);
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

        // get stored acceleration status:
	//    bit 0:  OFF (0) or ON (1)
	//    bit 1:  without planner (0) or with planner (1)
	//    bit 2:  unstrangled (0) or strangled (1)
	@Override
        public byte getAccelerationStatus(){
		byte[] val = readFromEEPROM(JettyG3EEPROM.STEPPER_DRIVER, 1);
		return val[0];
        }

        @Override
        // set stored acceleration status
        // acceleration is applied to all moves, except homing when ON
        public void setAccelerationStatus(byte status){
		byte b[] = new byte[1];

		// Only 3 bits are presently used
		status &= (byte)0x07;

		// If the accelerated planner is disabled, then force bits 1 and 2 off
		//   may not be the best idea
		if ((byte)0 == (status & (byte)0x01))
			status = (byte)0;
		b[0] = status;
		writeToEEPROM(JettyG3EEPROM.STEPPER_DRIVER, b);
        }

	// Unhandled:  FILAMENT_USED
	// Unhandled:  FILAMENT_USED_TRIP
	// Unhandled:  STEPS_PER_MM_A
	// Unhandled:  STEPS_PER_MM_B
	// Unhandled:  AXIS_HOME_POSITIONS
	// Unhandled:  STEPS_PER_MM_Y
	// Unhandled:  STEPS_PER_MM_X
	// Unhandled:  STEPS_PER_MM_Z
	// Unhandled:  MACHINE_NAME

	// The "Int" EEPROM parameters are actually uint8_t (aka, unsigned char)
	// There's no useful equivalent in Java so we promote these to Int

	@Override
	public int getEEPROMParamInt(EEPROMParams param) {
		switch (param) {
		case ABP_COPIES                 : return getUInt8EEPROM(JettyG3EEPROM.ABP_COPIES);
		case AXIS_INVERSION             : return getUInt8EEPROM(JettyG3EEPROM.AXIS_INVERSION);
		case BUZZER_REPEATS             : return getUInt8EEPROM(JettyG3EEPROM.BUZZER_REPEATS);
		case ENDSTOPS_USED              : return getUInt8EEPROM(JettyG3EEPROM.ENDSTOPS_USED);
		case ENDSTOP_INVERSION          : return getUInt8EEPROM(JettyG3EEPROM.ENDSTOP_INVERSION);
		case ESTOP_CONFIGURATION        : return getUInt8EEPROM(JettyG3EEPROM.ESTOP_CONFIGURATION);
		case EXTRUDE_DURATION           : return getUInt8EEPROM(JettyG3EEPROM.EXTRUDE_DURATION);
		case EXTRUDE_MMS                : return getUInt8EEPROM(JettyG3EEPROM.EXTRUDE_MMS);
		case INVERTED_EXTRUDER_5D       : return getUInt8EEPROM(JettyG3EEPROM.INVERTED_EXTRUDER_5D);
		case JOG_MODE_SETTINGS          : return getUInt8EEPROM(JettyG3EEPROM.JOG_MODE_SETTINGS);
		case LCD_TYPE                   : return getUInt8EEPROM(JettyG3EEPROM.LCD_TYPE);
		case MOOD_LIGHT_CUSTOM_BLUE     : return getUInt8EEPROM(JettyG3EEPROM.MOOD_LIGHT_CUSTOM_BLUE);
		case MOOD_LIGHT_CUSTOM_GREEN    : return getUInt8EEPROM(JettyG3EEPROM.MOOD_LIGHT_CUSTOM_GREEN);
		case MOOD_LIGHT_CUSTOM_RED      : return getUInt8EEPROM(JettyG3EEPROM.MOOD_LIGHT_CUSTOM_RED);
		case MOOD_LIGHT_SCRIPT          : return getUInt8EEPROM(JettyG3EEPROM.MOOD_LIGHT_SCRIPT);
		case OVERRIDE_GCODE_TEMP        : return getUInt8EEPROM(JettyG3EEPROM.OVERRIDE_GCODE_TEMP);
		case PLATFORM_TEMP              : return getUInt8EEPROM(JettyG3EEPROM.PLATFORM_TEMP);
		case PREHEAT_DURING_ESTIMATE    : return getUInt8EEPROM(JettyG3EEPROM.PREHEAT_DURING_ESTIMATE);
		case STEPPER_DRIVER             : return getUInt8EEPROM(JettyG3EEPROM.STEPPER_DRIVER);
		case TOOL0_TEMP                 : return getUInt8EEPROM(JettyG3EEPROM.TOOL0_TEMP);
		case TOOL1_TEMP                 : return getUInt8EEPROM(JettyG3EEPROM.TOOL1_TEMP);
		case VERSION_HIGH               : return getUInt8EEPROM(JettyG3EEPROM.VERSION_HIGH);
		case VERSION_LOW                : return getUInt8EEPROM(JettyG3EEPROM.VERSION_LOW);
		default :
			Base.logger.log(Level.WARNING, "getEEPROMParamInt(" + param + ") call failed");
			return 0;
		}
	}

	@Override
	public long getEEPROMParamUInt(EEPROMParams param) {
		switch (param) {
		case ACCEL_CLOCKWISE_EXTRUDER   : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_CLOCKWISE_EXTRUDER);
		case ACCEL_MAX_ACCELERATION_A   : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_ACCELERATION_A);
		case ACCEL_MAX_ACCELERATION_X   : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_ACCELERATION_X);
		case ACCEL_MAX_ACCELERATION_Y   : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_ACCELERATION_Y);
		case ACCEL_MAX_ACCELERATION_Z   : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_ACCELERATION_Z);
		case ACCEL_MAX_EXTRUDER_NORM    : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_EXTRUDER_NORM);
		case ACCEL_MAX_EXTRUDER_RETRACT : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_EXTRUDER_RETRACT);
		case ACCEL_MAX_FEEDRATE_A       : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_A);
		case ACCEL_MAX_FEEDRATE_B       : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_B);
		case ACCEL_MAX_FEEDRATE_X       : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_X);
		case ACCEL_MAX_FEEDRATE_Y       : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_Y);
		case ACCEL_MAX_FEEDRATE_Z       : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_Z);
		case ACCEL_MIN_PLANNER_SPEED    : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_MIN_PLANNER_SPEED);
		case ACCEL_REV_MAX_FEED_RATE    : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_REV_MAX_FEED_RATE);
		case ACCEL_SLOWDOWN_LIMIT       : return getUInt32EEPROM(JettyG3EEPROM.ACCEL_SLOWDOWN_LIMIT);
		case HOMING_FEED_RATE_X         : return getUInt32EEPROM(JettyG3EEPROM.HOMING_FEED_RATE_X);
		case HOMING_FEED_RATE_Y         : return getUInt32EEPROM(JettyG3EEPROM.HOMING_FEED_RATE_Y);
		case HOMING_FEED_RATE_Z         : return getUInt32EEPROM(JettyG3EEPROM.HOMING_FEED_RATE_Z);
		case RAM_USAGE_DEBUG            : return getUInt32EEPROM(JettyG3EEPROM.RAM_USAGE_DEBUG);
		default :
			Base.logger.log(Level.WARNING, "getEEPROMParamUInt(" + param + ") call failed");
			return 0L;
		}
	}

	@Override
	public double getEEPROMParamFloat(EEPROMParams param) {
		switch (param) {
		case ACCEL_ADVANCE_K            : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_ADVANCE_K) / 100000.0d;
		case ACCEL_ADVANCE_K2           : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_ADVANCE_K2) / 100000.0d;
		case ACCEL_EXTRUDER_DEPRIME_A   : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_EXTRUDER_DEPRIME) / 10.0d;
		case ACCEL_E_STEPS_PER_MM       : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_E_STEPS_PER_MM) / 10.0d;
		case ACCEL_MAX_SPEED_CHANGE_A   : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_SPEED_CHANGE_A) / 10.0d;
		case ACCEL_MAX_SPEED_CHANGE_X   : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_SPEED_CHANGE_X) / 10.0d;
		case ACCEL_MAX_SPEED_CHANGE_Y   : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_SPEED_CHANGE_Y) / 10.0d;
		case ACCEL_MAX_SPEED_CHANGE_Z   : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_SPEED_CHANGE_Z) / 10.0d;
		case ACCEL_MIN_FEED_RATE        : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_MIN_FEED_RATE) / 10.0d;
		case ACCEL_MIN_SEGMENT_TIME     : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_MIN_SEGMENT_TIME) / 10000.0d;
		case ACCEL_MIN_TRAVEL_FEED_RATE : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_MIN_TRAVEL_FEED_RATE) / 10.0d;
		case ACCEL_NOODLE_DIAMETER      : return (double)getUInt32EEPROM(JettyG3EEPROM.ACCEL_NOODLE_DIAMETER) / 100.0d;
		default :
			Base.logger.log(Level.WARNING, "getEEPROMParamFloat(" + param + ") call failed");
			return 0d;
		}
	}

	// Unhandled:  FILAMENT_USED
	// Unhandled:  FILAMENT_USED_TRIP
	// Unhandled:  STEPS_PER_MM_A
	// Unhandled:  STEPS_PER_MM_B
	// Unhandled:  AXIS_HOME_POSITIONS
	// Unhandled:  STEPS_PER_MM_Y
	// Unhandled:  STEPS_PER_MM_X
	// Unhandled:  STEPS_PER_MM_Z
	// Unhandled:  MACHINE_NAME

	@Override
	public void setEEPROMParam(EEPROMParams param, int val) {
		if (val < 0)
			val = 0;
		switch (param) {
		case ABP_COPIES                 : setUInt8EEPROM(JettyG3EEPROM.ABP_COPIES, val); break;
		case AXIS_INVERSION             : setUInt8EEPROM(JettyG3EEPROM.AXIS_INVERSION, val); break;
		case BUZZER_REPEATS             : setUInt8EEPROM(JettyG3EEPROM.BUZZER_REPEATS, val); break;
		case ENDSTOPS_USED              : setUInt8EEPROM(JettyG3EEPROM.ENDSTOPS_USED, val); break;
		case ENDSTOP_INVERSION          : setUInt8EEPROM(JettyG3EEPROM.ENDSTOP_INVERSION, val); break;
		case ESTOP_CONFIGURATION        : setUInt8EEPROM(JettyG3EEPROM.ESTOP_CONFIGURATION, val); break;
		case EXTRUDE_DURATION           : setUInt8EEPROM(JettyG3EEPROM.EXTRUDE_DURATION, val); break;
		case EXTRUDE_MMS                : setUInt8EEPROM(JettyG3EEPROM.EXTRUDE_MMS, val); break;
		case INVERTED_EXTRUDER_5D       : setUInt8EEPROM(JettyG3EEPROM.INVERTED_EXTRUDER_5D, val); break;
		case JOG_MODE_SETTINGS          : setUInt8EEPROM(JettyG3EEPROM.JOG_MODE_SETTINGS, val); break;
		case LCD_TYPE                   : setUInt8EEPROM(JettyG3EEPROM.LCD_TYPE, val); break;
		case MOOD_LIGHT_CUSTOM_BLUE     : setUInt8EEPROM(JettyG3EEPROM.MOOD_LIGHT_CUSTOM_BLUE, val); break;
		case MOOD_LIGHT_CUSTOM_GREEN    : setUInt8EEPROM(JettyG3EEPROM.MOOD_LIGHT_CUSTOM_GREEN, val); break;
		case MOOD_LIGHT_CUSTOM_RED      : setUInt8EEPROM(JettyG3EEPROM.MOOD_LIGHT_CUSTOM_RED, val); break;
		case MOOD_LIGHT_SCRIPT          : setUInt8EEPROM(JettyG3EEPROM.MOOD_LIGHT_SCRIPT, val); break;
		case OVERRIDE_GCODE_TEMP        : setUInt8EEPROM(JettyG3EEPROM.OVERRIDE_GCODE_TEMP, val); break;
		case PLATFORM_TEMP              : setUInt8EEPROM(JettyG3EEPROM.PLATFORM_TEMP, val); break;
		case PREHEAT_DURING_ESTIMATE    : setUInt8EEPROM(JettyG3EEPROM.PREHEAT_DURING_ESTIMATE, val); break;
		case STEPPER_DRIVER             : setUInt8EEPROM(JettyG3EEPROM.STEPPER_DRIVER, val); break;
		case TOOL0_TEMP                 : setUInt8EEPROM(JettyG3EEPROM.TOOL0_TEMP, val); break;
		case TOOL1_TEMP                 : setUInt8EEPROM(JettyG3EEPROM.TOOL1_TEMP, val); break;
		case VERSION_HIGH               : setUInt8EEPROM(JettyG3EEPROM.VERSION_HIGH, val); break;
		case VERSION_LOW                : setUInt8EEPROM(JettyG3EEPROM.VERSION_LOW, val); break;
		default : Base.logger.log(Level.WARNING, "setEEPROMParam(" + param + ", " + val + ") call failed"); break;
		}
	}

	@Override
	public void setEEPROMParam(EEPROMParams param, long val) {
		if (val < 0L)
			val = 0L;
		switch (param) {
		case ACCEL_CLOCKWISE_EXTRUDER   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_CLOCKWISE_EXTRUDER, val); break;
		case ACCEL_MAX_ACCELERATION_A   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_ACCELERATION_A, val); break;
		case ACCEL_MAX_ACCELERATION_X   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_ACCELERATION_X, val); break;
		case ACCEL_MAX_ACCELERATION_Y   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_ACCELERATION_Y, val); break;
		case ACCEL_MAX_ACCELERATION_Z   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_ACCELERATION_Z, val); break;
		case ACCEL_MAX_EXTRUDER_NORM    : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_EXTRUDER_NORM, val); break;
		case ACCEL_MAX_EXTRUDER_RETRACT : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_EXTRUDER_RETRACT, val); break;
		case ACCEL_MAX_FEEDRATE_A       : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_A, val); break;
		case ACCEL_MAX_FEEDRATE_B       : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_B, val); break;
		case ACCEL_MAX_FEEDRATE_X       : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_X, val); break;
		case ACCEL_MAX_FEEDRATE_Y       : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_Y, val); break;
		case ACCEL_MAX_FEEDRATE_Z       : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_FEEDRATE_Z, val); break;
		case ACCEL_MIN_PLANNER_SPEED    : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MIN_PLANNER_SPEED, val); break;
		case ACCEL_REV_MAX_FEED_RATE    : setUInt32EEPROM(JettyG3EEPROM.ACCEL_REV_MAX_FEED_RATE, val); break;
		case ACCEL_SLOWDOWN_LIMIT       : setUInt32EEPROM(JettyG3EEPROM.ACCEL_SLOWDOWN_LIMIT, val); break;
		case HOMING_FEED_RATE_X         : setUInt32EEPROM(JettyG3EEPROM.HOMING_FEED_RATE_X, val); break;
		case HOMING_FEED_RATE_Y         : setUInt32EEPROM(JettyG3EEPROM.HOMING_FEED_RATE_Y, val); break;
		case HOMING_FEED_RATE_Z         : setUInt32EEPROM(JettyG3EEPROM.HOMING_FEED_RATE_Z, val); break;
		case RAM_USAGE_DEBUG            : setUInt32EEPROM(JettyG3EEPROM.RAM_USAGE_DEBUG, val); break;
		default : Base.logger.log(Level.WARNING, "setEEPROMParam(" + param + ", " + val + ") call failed"); break;
		}
	}

	@Override
	public void setEEPROMParam(EEPROMParams param, double val) {
		if (val < 0.0d)
			val = 0.0d;
		switch (param) {
		case ACCEL_ADVANCE_K            : setUInt32EEPROM(JettyG3EEPROM.ACCEL_ADVANCE_K, (long)(val * 100000.0d)); break;
		case ACCEL_ADVANCE_K2           : setUInt32EEPROM(JettyG3EEPROM.ACCEL_ADVANCE_K2, (long)(val * 100000.0d)); break;
		case ACCEL_EXTRUDER_DEPRIME_A   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_EXTRUDER_DEPRIME, (long)(val * 10.0d)); break;
		case ACCEL_E_STEPS_PER_MM       : setUInt32EEPROM(JettyG3EEPROM.ACCEL_E_STEPS_PER_MM, (long)(val * 10.0d)); break;
		case ACCEL_MAX_SPEED_CHANGE_A   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_SPEED_CHANGE_A, (long)(val * 10.0d)); break;
		case ACCEL_MAX_SPEED_CHANGE_X   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_SPEED_CHANGE_X, (long)(val * 10.0d)); break;
		case ACCEL_MAX_SPEED_CHANGE_Y   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_SPEED_CHANGE_Y, (long)(val * 10.0d)); break;
		case ACCEL_MAX_SPEED_CHANGE_Z   : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MAX_SPEED_CHANGE_Z, (long)(val * 10.0d)); break;
		case ACCEL_MIN_FEED_RATE        : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MIN_FEED_RATE, (long)(val * 10.0d)); break;
		case ACCEL_MIN_SEGMENT_TIME     : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MIN_SEGMENT_TIME, (long)(val * 10000.0d)); break;
		case ACCEL_MIN_TRAVEL_FEED_RATE : setUInt32EEPROM(JettyG3EEPROM.ACCEL_MIN_TRAVEL_FEED_RATE, (long)(val * 10.0d)); break;
		case ACCEL_NOODLE_DIAMETER      : setUInt32EEPROM(JettyG3EEPROM.ACCEL_NOODLE_DIAMETER, (long)(val * 100.0d)); break;
		default : Base.logger.log(Level.WARNING, "setEEPROMParam(" + param + ", " + val + ") call failed"); break;
		}
	}

	/**
	 * Reset to the factory state
	 * @throws RetryException 
	 */
	@Override
	public void resetSettingsToFactory() throws RetryException {
		Base.logger.finer("resetting to factory in Makerbot4G");
		if (hasAcceleration()) {
			/// Send message to the firmware to restore EEPROM parameters to their default values
			/// Not reset are the filament counters and the home offsets
			PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.RESET_TO_FACTORY.getCode());
			pb.add8((byte) 0xFF);  // reserved byte in payload
			PacketResponse pr = runCommand(pb.getPacket());
		}
		else
			super.resetSettingsToBlank();
	}

}
