package replicatorg.drivers.gen3;

import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import org.w3c.dom.Element;

import replicatorg.app.Base;
import replicatorg.drivers.RetryException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.MachineModel;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;

public class Makerbot4GAlternateDriver extends Makerbot4GDriver {

	public String getDriverName() {
		return "Makerbot4GAlternate";
	}

	/**
	 * Overloaded to manage a hijacked axis and run this axis in relative mode instead of the extruder DC motor
	 */
	public void queuePoint(Point5d p) throws RetryException {

		// Filter away any hijacked axes from the given point.
		// This is necessary to avoid taking deltas into account where we 
		// compare the relative p coordinate (usually 0) with the absolute 
		// currentPosition (which we get from the Motherboard).
		Point5d filteredpoint = new Point5d(p);
		Point5d filteredcurrent = new Point5d(getCurrentPosition());
		for (AxisId axis : getHijackedAxes()) {
			filteredpoint.setAxis(axis, 0d);
			filteredcurrent.setAxis(axis, 0d);
		}
		
		// is this point even step-worthy? Only compute nonzero moves
		Point5d deltaSteps = getAbsDeltaSteps(filteredcurrent, filteredpoint);
		if (deltaSteps.length() > 0.0) {
			Point5d delta = new Point5d();
			delta.sub(filteredpoint, filteredcurrent); // delta = p - current
			delta.absolute(); // absolute value of each component
			
			double feedrate = getSafeFeedrate(delta); // FIXME: Doesn't take max feedrate of the extruder into account
			
			// Calculate time for move in usec
			double minutes = delta.length() / feedrate;

			Point5d steps = machine.mmToSteps(filteredpoint);		
			int relative = modifyHijackedAxes(steps, minutes);

			// okay, send it off!
			queueNewPoint(steps, (long) (60 * 1000 * 1000 * minutes), relative);

			setInternalPosition(filteredpoint);
		}
	}

	/**
	 * Overloaded to support extruding without moving by converting a delay in to an extruder command
	 */
	public void delay(long millis) throws RetryException {
		if (Base.logger.isLoggable(Level.FINER)) {
			Base.logger.log(Level.FINER,"Delaying " + millis + " millis.");
		}

		Point5d steps = new Point5d();
		modifyHijackedAxes(steps, millis / 60000d);

		if (steps.length() > 0) {
			queueNewPoint(steps, millis * 1000, 0x1f); // All axes relative to avoid dealing with absolute coords
		}
		else {
			super.delay(millis); // This resulted in no stepper movements -> fall back to normal delay
		}
	}
	
	/** 
	 * Returns the hijacked axes for the current tool.
	 */
	private Iterable<AxisId> getHijackedAxes() {
		Vector<AxisId> axes = new Vector<AxisId>();
		for ( Map.Entry<AxisId,ToolModel> entry : stepExtruderMap.entrySet()) {
			ToolModel curTool = machine.currentTool();
			AxisId axis = entry.getKey();
			if (curTool.equals(entry.getValue())) {
				axes.add(axis);
			}
		}
		return axes;
	}

	/**
	 * Write a relative movement to any axes which has been hijacked where the extruder is turned on.
	 * The axis will be moved with a length corresponding to the current RPM and the duration of the movement.
	 * If the extruder is off, the corresponding axes are set to a zero relative movement.
	 * @param steps
	 * @param minutes
	 * @return a bitmask with the relative bit set for all hijacked axes
	 */
	private int modifyHijackedAxes(Point5d steps, double minutes) {
		int relative = 0;

		for (AxisId axis : getHijackedAxes()) {
			relative |= 1 << axis.getIndex();
			double extruderSteps = 0;
			ToolModel curTool = machine.currentTool();
			if (curTool.isMotorEnabled()) {
				// Figure out length of move
				final double extruderStepsPerMinute = curTool.getMotorSpeedRPM()*curTool.getMotorSteps();
				final boolean clockwise = machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE;
				extruderSteps = (clockwise?-1d:1d) * extruderStepsPerMinute * minutes;
			}
			steps.setAxis(axis, extruderSteps);
		}
		return relative;
	}

	protected void queueNewPoint(Point5d steps, long us, int relative) throws RetryException {
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.QUEUE_POINT_NEW.getCode());

		if (Base.logger.isLoggable(Level.FINE)) {
			Base.logger.log(Level.FINE,"Queued new-style point " + steps + " over "
					+ Long.toString(us) + " usec., relative " + Integer.toString(relative));
		}

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
	
	EnumMap<AxisId,ToolModel> stepExtruderMap = new EnumMap<AxisId,ToolModel>(AxisId.class);
	
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
						stepExtruderMap.put(axis,tm);
						m.getAvailableAxes().remove(axis);
					} else {
						Base.logger.severe("Tool claims unavailable axis "+axis.name());
					}
				} catch (IllegalArgumentException iae) {
					Base.logger.severe("Unintelligible axis designator "+stepAxisStr);
				}
			}
		}
	}
}
