package replicatorg.drivers.gen3;

import java.util.EnumMap;
import java.util.Map;
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

	public void queuePoint(Point5d p) throws RetryException {
		// is this point even step-worthy?
		Point5d deltaSteps = getAbsDeltaSteps(getCurrentPosition(), p);
		double length = deltaSteps.length();
		if (length > 0.0) { // only compute nonzero moves
			// where we going?
			Point5d steps = machine.mmToSteps(p);
			
			Point5d delta = getDelta(p);
			double feedrate = getSafeFeedrate(delta);
			
			// Calculate time for move in usec
			double minutes = delta.length() / feedrate;
			double us = (60.0 * 1000.0 * 1000.0) * minutes;

			int relative = 0;
			// Modify hijacked axes
			for (Map.Entry<AxisId,ToolModel> entry : stepExtruderMap.entrySet()) {
				ToolModel curTool = machine.currentTool();
				final AxisId axis = entry.getKey();
				relative |= 1 << axis.getIndex();
				if (curTool.equals(entry.getValue()) && curTool.isMotorEnabled()) {
					// Figure out length of move
					final double extruderStepsPerMinute = curTool.getMotorSpeedRPM()*curTool.getMotorSteps();
					final boolean clockwise = machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE;
					final double extruderSteps = (clockwise?-1d:1d) * extruderStepsPerMinute * minutes;
					steps.setAxis(axis, extruderSteps);
				} else {
					p.setAxis(axis, 0);
				}
			}

			// okay, send it off!
			queueNewPoint(steps, (long)us, relative);

			setInternalPosition(p);
		}
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
