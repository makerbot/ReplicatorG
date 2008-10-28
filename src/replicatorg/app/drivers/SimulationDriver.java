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

package replicatorg.app.drivers;

import javax.vecmath.Point3d;

import replicatorg.app.SimulationWindow;
import replicatorg.app.SimulationWindow2D;
import replicatorg.app.exceptions.GCodeException;

public class SimulationDriver extends DriverBaseImplementation
{
	private SimulationWindow simulation;
	
	public SimulationDriver()
	{
		super();
	}
	
	public void createWindow()
	{
		simulation = new SimulationWindow2D();
		simulation.setVisible(true);
	}
	
	public void dispose()
	{
		super.dispose();
		hideWindow();
	}
	
	public void hideWindow()
	{
		simulation.setVisible(false);
		simulation.dispose();
		simulation = null;
	}
	
	public void execute()
	{
		//suppress any errors
		try {
			super.execute();
		} catch (GCodeException e) {}
	}
	
	public void queuePoint(Point3d p)
	{
		simulation.queuePoint(p);
		
		super.queuePoint(p);
	}
	
	public void homeXYZ()
	{
		queuePoint(new Point3d());
	}
	
	public void homeXY()
	{
		Point3d pos = getCurrentPosition();
		pos.x = 0;
		pos.y = 0;
		
		queuePoint(pos);
	}
	
	public void homeX()
	{
		Point3d pos = getCurrentPosition();
		pos.x = 0;
		
		queuePoint(pos);
	}

	public void homeY()
	{
		Point3d pos = getCurrentPosition();
		pos.y = 0;
		
		queuePoint(pos);
	}
	
	public void homeZ()
	{
		Point3d pos = getCurrentPosition();
		pos.z = 0;
		
		queuePoint(pos);
	}
}
