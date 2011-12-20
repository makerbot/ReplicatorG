package replicatorg.drivers.gen3;

import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import javax.vecmath.Point3d;

import org.w3c.dom.Element;

import replicatorg.app.Base;
import replicatorg.drivers.RetryException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.MachineModel;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;

public class Makerbot4GDriver extends Sanguino3GDriver {

	private boolean stepperExtruderFanEnabled = false;

	public String getDriverName() {
		return "Makerbot4G";
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
	public void setMotorRPM(double rpm) throws RetryException {
		machine.currentTool().setMotorSpeedRPM(rpm);
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


	
}
