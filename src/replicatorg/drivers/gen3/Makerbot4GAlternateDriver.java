package replicatorg.drivers.gen3;

import java.util.Map;
import java.util.Vector;

import javax.vecmath.Point3d;

import replicatorg.app.Base;
import replicatorg.drivers.RetryException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.MachineModel;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;

public class Makerbot4GAlternateDriver extends Makerbot4GDriver {

	@Override public String getDriverName() {
		return "Makerbot4GAlternate";
	}
	
	/**
	 * Overloaded to manage a hijacked axis and run this axis in relative mode instead of the extruder DC motor
	 */
	@Override public void queuePoint(Point5d p) throws RetryException {
		// If we don't know our current position, make this move an old-style move, ignoring any hijacked axes. 
		if (positionLost()) {
			Base.logger.fine("Position invalid, reverting to default speed for next motion");
			// Filter away any hijacked axes from the given point.
			Point5d filteredPoint = new Point5d(p);
			long longestDDA = 0;
			
			for (AxisId axis : getAllHijackedAxes()) {
				filteredPoint.setAxis(axis, 0d);
			}
			
			// Find the slowest DDA of all motion axes
			Point5d maxFeedrates = machine.getMaximumFeedrates();
			Point5d stepsPerMM = machine.getStepsPerMM();
			for (AxisId axis : machine.getAvailableAxes()) {
				long axisDDA = (long) (60*1000000/(maxFeedrates.axis(axis)*stepsPerMM.axis(axis)));
				
				Base.logger.info("For axis " + axis.getIndex()
						+ ", maxFeedrate="
						+ maxFeedrates.axis(axis)
						+ ", stepsPerMM=" + stepsPerMM.axis(axis)
						+ "DDA:" + axisDDA);
				
				if (axisDDA > longestDDA) {
					longestDDA = axisDDA;
				}
			}
			
			// okay, send it off!
			// TODO: bug: We move all axes (even ones that shouldn't be moved) How to avoid?
			queueAbsolutePoint(machine.mmToSteps(filteredPoint), longestDDA);
			// Finally, recored the position, and mark it as valid.
			setInternalPosition(filteredPoint);
		} else {
			// Filter away any hijacked axes from the given point.
			// This is necessary to avoid taking deltas into account where we 
			// compare the relative p coordinate (usually 0) with the absolute 
			// currentPosition (which we get from the Motherboard).
			Point5d filteredpoint = new Point5d(p);
			Point5d filteredcurrent = new Point5d(getCurrentPosition(false));
			int relative = 0;
			for (AxisId axis : getAllHijackedAxes()) {
				filteredpoint.setAxis(axis, 0d);
				filteredcurrent.setAxis(axis, 0d);
				relative |= 1 << axis.getIndex();
			}
		
			// is this point even step-worthy? Only compute nonzero moves
			Point5d deltaSteps = getAbsDeltaSteps(filteredcurrent, filteredpoint);
			if (deltaSteps.length() > 0.0) {
				Point5d delta = new Point5d();
				delta.sub(filteredpoint, filteredcurrent); // delta = p - current
				delta.absolute(); // absolute value of each component
				
				Point5d axesmovement = calcHijackedAxesMovement(delta);
				delta.add(axesmovement);
				filteredpoint.add(axesmovement);
				
				// Calculate time for move in usec
				Point5d steps = machine.mmToSteps(filteredpoint);		
	
				// okay, send it off!
				// The 4. and 5. dimensions doesn't have a spatial interpretation. Calculate time in 3D space
				double minutes = delta.get3D().distance(new Point3d())/ getSafeFeedrate(delta);
				
				queueNewPoint(steps, (long) (60 * 1000 * 1000 * minutes), relative);

				setInternalPosition(filteredpoint);
			}
		}
	}

	/**
	 * In legacy use, delayed while a DC motor ran. For compatiblity, this function
	 * creates a Point5D to do 'extrude while sitting still'. 
	 */
	@Override public void delay(long millis) throws RetryException {
		Base.logger.finer("Delaying " + millis + " millis.");	
	
		Point5d steps = pointsFromHijackedAxes( machine.currentTool(), millis / 60000d);

		/// All axes relative to avoid dealing with absolute coords
		if (steps.length() > 0) {
			queueNewPoint(steps, millis * 1000, 0x1f); 
		}
		// This resulted in no stepper movements -> fall back to normal delay
		else {
			super.delay(millis); 
		}
	}
	
	/** 
	 * Each Axis can be overridden (hijacked) by XML settings. Return all overridden
	 * axes associated with tool curTool.
	 * @param curTool target tool to find overrides for
	 * @return a list of AxisId containing all overridden axis for tool curTool
	 */
	protected Iterable<AxisId> getHijackedAxes(ToolModel curTool) {
		//NOTE:
		/// can be reduced to '		axes.add(curTool.getMotorStepperAxis());' 
		/// once we get rid of curTool calls
		Vector<AxisId> axes = new Vector<AxisId>();
		for ( Map.Entry<AxisId,ToolModel> entry : extruderHijackedMap.entrySet()) {
			AxisId axis = entry.getKey();
			if (curTool.equals(entry.getValue())) {
				axes.add(axis);
			}
		}
		return axes;
	}

	
	/**
	 * Calculate and return the corresponding movement of any hijacked axes where the extruder is on.
	 * The returned movement is in mm of incoming filament (corresponding to mm in machines.xml)
	 * If the extruder is off, the hijacked axes are not moved.
	 * @param delta relative XYZ movement.
	 * @return The relative movement (in mm) of the hijacked axes
	 */
	private Point5d calcHijackedAxesMovement(Point5d delta) {

		Point5d movement = new Point5d();
		double minutes = delta.length() / getCurrentFeedrate();

		for (AxisId axis : getHijackedAxes(machine.currentTool()) ) {
			ToolModel curTool = machine.currentTool();
			if (curTool.isMotorEnabled()) {
				double extruderStepsPerMinute = curTool.getMotorSpeedRPM() * curTool.getMotorSteps();
				final boolean clockwise = machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE;
				movement.setAxis(axis, extruderStepsPerMinute * minutes / machine.getStepsPerMM().axis(axis) * (clockwise?-1d:1d));
			}
		}
		return movement;
	}

	/**
	 * Generates a Point5D for extrusion based on if an axis is hijacked, if a motor is enabled
	 * and if the axis is created properly. 
	 * Write a relative movement to any axes which has been hijacked where the extruder is turned on.
	 * The axis will be moved with a length corresponding to the duration of the movement.
	 * The speed of the hijacked axis will be clamped to its maximum feedrate.
	 * If the extruder is off, the corresponding axes are set to a zero relative movement.
	 * @param curTool target tool to check for axis hijacking
	 * @param minutes extrusion time 
	 * @return 	If a the axis for curTool is 'hijacked' and enabled returns a Point5d for extruding for time set 
	 * by @minutes. If no axis is enabled and hijacked, returns an 'empty' Point5d with no motion.
	 */
	private Point5d pointsFromHijackedAxes(ToolModel curTool, double minutes) {
		Point5d steps = new Point5d();
		Base.logger.finer("modify hijacked axes");
		
		for (AxisId axis : getHijackedAxes(machine.currentTool())) {
			Base.logger.finer("modify hijacked axes doing " + axis.toString() );
			double extruderSteps = 0;
			
			if (curTool.isMotorEnabled()) {
				Base.logger.finer("modify hijacked axes doing enabled stuff" + axis.toString() );
				double maxrpm = machine.getMaximumFeedrates().axis(axis) * machine.getStepsPerMM().axis(axis) / curTool.getMotorSteps();
				double rpm = (curTool.getMotorSpeedRPM() > maxrpm) ? maxrpm : curTool.getMotorSpeedRPM();
				boolean clockwise = machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE;
				extruderSteps = rpm * curTool.getMotorSteps() * minutes * (clockwise?-1d:1d);
			}
			
			Base.logger.finer("setting axis " + axis.toString() );
			Base.logger.finer("setting extruderSteps" + Double.toString(extruderSteps) );
			steps.setAxis(axis, extruderSteps);
		}
		return steps;
	}

	protected void queueNewPoint(Point5d steps, long us, int relative) throws RetryException {
		Base.logger.finer("Makerbot4GAlternateDriver queueNewPoint");

		// Turn on fan if necessary
		for (AxisId axis : getHijackedAxes( machine.currentTool()) ) {
			if (steps.axis(axis) != 0) {
				enableStepperExtruderFan(true, machine.currentTool().getIndex());
			}
		}
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.QUEUE_POINT_NEW.getCode());

		Base.logger.finer("Queued new-style point " + steps + " over "
					+ Long.toString(us) + " usec., relative " + Integer.toString(relative));


		// just add them in now.
		pb.add32((int) steps.x());
		pb.add32((int) steps.y());
		pb.add32((int) steps.z());
		pb.add32((int) steps.a());
		pb.add32((int) steps.b());
		pb.add32((int) us);
		pb.add8((int) relative);

		runCommand(pb.getPacket());
	}

	/**
	 * When the machine is set for this driver, some toolheads may poach the an extrusion axis.
	 */
	@Override public void setMachine(MachineModel m) {
		super.setMachine(m);
		
		// Curiously, this driver removes axes from the available list;
		// the parent does not.
		for (AxisId axis : extruderHijackedMap.keySet()) {
			m.getAvailableAxes().remove(axis);
		}
	}
	
	/**
	 * Overridden to not ask the board for the RPM as it would report the RPM from the extruder 
	 * controller, which doesn't know about it in this case.
	 */
	@Override public double getMotorRPM(int toolhead) {
		ToolModel tool = machine.getTool(toolhead);
		double rpm = tool.getMotorSpeedRPM();
		tool.setMotorSpeedReadingRPM(rpm);
		return rpm;
	}
	
	@Override 
	public String getMachineType(){ return "Thing-O-Matic/CupCake CNC"; } 
}
