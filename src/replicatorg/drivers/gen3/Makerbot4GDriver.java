package replicatorg.drivers.gen3;

import java.util.logging.Level;

import replicatorg.app.Base;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.gen3.MotherboardCommandCode;
import replicatorg.drivers.gen3.PacketBuilder;
import replicatorg.drivers.gen3.Sanguino3GDriver;
import replicatorg.util.Point5d;

public class Makerbot4GDriver extends Sanguino3GDriver {

	public String getDriverName() {
		return "Makerbot4G";
	}

	protected void queueAbsolutePoint(Point5d steps, long micros) throws RetryException {
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
		
		this.currentPosition.set(p);
	}

	protected Point5d reconcilePosition() {
		// If we're writing to a file, we can't actually know what the current position is.
		if (fileCaptureOstream != null) {
			return null;
		}
		PacketBuilder pb = new PacketBuilder(MotherboardCommandCode.GET_POSITION_EXT.getCode());
		PacketResponse pr = runQuery(pb.getPacket());
		Point5d steps = new Point5d(pr.get32(), pr.get32(), pr.get32(), pr.get32(), pr.get32());
//		Base.logger.fine("Reconciling : "+machine.stepsToMM(steps).toString());
		return machine.stepsToMM(steps);
	}

}
