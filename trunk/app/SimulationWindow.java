/*
  Part of the ReplicatorG project - http://www.replicat.org
  Copyright (c) 2008 Zach Smith

  Forked from Arduino: http://www.arduino.cc

  Based on Processing http://www.processing.org
  Copyright (c) 2004-05 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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
  
  $Id: Editor.java 370 2008-01-19 16:37:19Z mellis $
*/

package processing.app;

import javax.swing.*;
import java.awt.*;
import javax.vecmath.*;

public class SimulationWindow extends JFrame
{
	private int myWidth = 0;
	private int myHeight = 0;
	
	protected Point3d minimum;
	protected Point3d maximum;
	
	public SimulationWindow ()
	{
		super("Build Simulation");
		
		//make it most of our screen.
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		myWidth = screen.width-40;
		if (myWidth > 1024)
			myWidth = 1024;
		myHeight = screen.height-40;
		if (myHeight > 768)
			myHeight = 768;
			
	 	this.setBounds(20, 20, myWidth, myHeight);
	
		//no resizing... yet
		this.setResizable(false);
		
		//no menu bar.
		this.setMenuBar(null);
		
		//init our bounds.
		minimum = new Point3d(0.0, 0.0, 0.0);
		maximum = new Point3d(0.0, 0.0, 0.0);
	}
	
	public void setMinimums(Point3d p)
	{
		minimum = p;
	}
	
	public void setMaximums(Point3d p)
	{
		maximum = p;
	}
	
	public void plotLine(Point3d start, Point3d end, boolean extruding)
	{
		repaint();
	}
	
	//TODO: make this
	public double convertRealXToPointX(double x)
	{
		// i think?
		// round(myWidth * (abs(min - x) / abs(max - min)))
		
		return x;
	}
	
	//todo: make this
	public double convertRealYtoPointY(double y)
	{
		// i think?
		// round(myHeight * (abs(min - y) / abs(max - min)))

		return y;
	}
	
	//todo: make this
	public double convertRealZtoPointZ(double z)
	{
		return z;
	}
}