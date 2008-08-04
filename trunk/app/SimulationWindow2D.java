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

import java.awt.*;
import javax.swing.*;
import java.util.*;
import javax.vecmath.*;

public class SimulationWindow2D extends SimulationWindow
{	
	SimulationCanvas2D canvas;
	
	public SimulationWindow2D ()
	{
		super();
		
		setTitle("2D Build Simulation");
		
		canvas = new SimulationCanvas2D();
		getContentPane().add(canvas);
	}
	
	synchronized public void queuePoint(Point3d point)
	{
		canvas.queuePoint(point);
	}
}

class SimulationCanvas2D extends Canvas
{
	protected Point3d minimum;
	protected Point3d maximum;
	protected double currentZ;
	
	protected Vector points;
	
	public SimulationCanvas2D()
	{
		super();
		
		//init our bounds.
		minimum = new Point3d();
		maximum = new Point3d();
		currentZ = 0.0;
		
		//initialize our vector
		points = new Vector();
		points.add(new Point3d());
		
		setBackground(Color.white);
		setForeground(Color.white);
		
		System.out.println("2d canvas build.");
	}
	
	synchronized public void queuePoint(Point3d point)
	{
		points.add(point);
		
		if (point.x < minimum.x)
			minimum.x = point.x;
		if (point.y < minimum.y)
			minimum.y = point.y;
		if (point.z < minimum.z)
			minimum.z = point.z;
			
		if (point.x > maximum.x)
			maximum.x = point.x;
		if (point.y > maximum.y)
			maximum.y = point.y;
		if (point.z > maximum.z)
			maximum.z = point.z;
			
		currentZ = point.z;

		resizeIfNeeded();
			
		repaint();
	}
	
	public void resizeIfNeeded()
	{
		
	}
	
	public void paint(Graphics g)
	{
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g2.setPaint(Color.black);

	    g.setFont(new Font("SansSerif", Font.PLAIN, 11));
	    g.setColor(Color.black);
		g.drawString("Layer at z:" + currentZ, 20, 20);
		
		Point3d start = null;
		Point3d current = null;
		
		//loop through points and draw them all.
		for (int i=0; i<points.size(); i++)
		{
			current = (Point3d)points.get(i);
			if (current.z == currentZ)
			{
				if (start == null)
				{
					start = current;
					continue;
				}
				else if (start.x != current.x || start.y != current.y)
				{
					int startX = convertRealXToPointX(start.x);
					int startY = convertRealYToPointY(start.y);
					int endX = convertRealXToPointX(current.x);
					int endY = convertRealYToPointY(current.y);

					System.out.println("line from " + startX + ", " + startY + " to " + endX + ", " + endX);
					g.drawLine(startX, startY, endX, endY);

					//save it for next time.
					start = current;
				}
			}
		}
	}
	
	public int convertRealXToPointX(double x)
	{
		return (int)Math.round(getWidth() * (Math.abs(minimum.x - x) / Math.abs(maximum.x - minimum.x)));
	}
	
	public int convertRealYToPointY(double y)
	{
		return (int)Math.round(getWidth() * (Math.abs(minimum.y - y) / Math.abs(maximum.y - minimum.y)));
	}
}