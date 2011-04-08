/*
 SerialPassthroughDriver.java

 This is a driver to control a machine that contains a GCode parser and communicates via Serial Port.

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

package replicatorg.drivers;

import java.awt.geom.Rectangle2D;

import replicatorg.app.ui.SimulationWindow;
import replicatorg.app.ui.SimulationWindow2D;
import replicatorg.util.Point5d;

public class SimulationDriver extends DriverBaseImplementation {
	private SimulationWindow simulation;

	public SimulationDriver() {
		super();
	}

	public void setSimulationBounds(Rectangle2D.Double bounds) {
		if (isSimulating()) {
			simulation.setSimulationBounds(bounds);
		}
	}
	
	public boolean isSimulating() {
		return simulation != null;
	}
	
	public void createWindow() {
		simulation = new SimulationWindow2D();
		simulation.setVisible(true);
	}

	public void destroyWindow() {
		if (simulation != null) {
			simulation.setVisible(false);
			simulation.dispose();
		}
		simulation = null;
	}

	public void dispose() {
		destroyWindow();
		super.dispose();
	}

	public void queuePoint(Point5d p) throws RetryException {
		simulation.queuePoint(p);

		super.queuePoint(p);
	}

	public void homeXYZ() throws RetryException {
		queuePoint(new Point5d());
	}

	public void homeXY() throws RetryException {
		Point5d pos = getCurrentPosition(false);
		pos.setX(0);
		pos.setY(0);

		queuePoint(pos);
	}

	public void homeX() throws RetryException {
		Point5d pos = getCurrentPosition(false);
		pos.setX(0);

		queuePoint(pos);
	}

	public void homeY() throws RetryException {
		Point5d pos = getCurrentPosition(false);
		pos.setY(0);

		queuePoint(pos);
	}

	public void homeZ() throws RetryException {
		Point5d pos = getCurrentPosition(false);
		pos.setZ(0);

		queuePoint(pos);
	}

	protected Point5d reconcilePosition() {
		// Initial position irrelevant for this driver; it's all relative to the start pos
		return new Point5d();
	}

}
