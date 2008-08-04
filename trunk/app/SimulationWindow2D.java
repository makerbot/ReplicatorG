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
	private Point3d minimum;
	private Point3d maximum;
	private double currentZ;
	
	private double ratio = 1.0;

	private Vector points;
	
	public SimulationCanvas2D()
	{
		super();
		
		//init our bounds.
		minimum = new Point3d();
		maximum = new Point3d();
		currentZ = 0.0;
		
		//initialize our vector
		points = new Vector();
		points.addElement(new Point3d());
		
		setBackground(Color.white);
		setForeground(Color.white);
	}
	
	synchronized public void queuePoint(Point3d point)
	{
		//System.out.println("queued: " + point.toString());
		
		Point3d myPoint = new Point3d(point);
		points.addElement(myPoint);
		
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

		calculateRatio();
			
		repaint();
	}
	
	public void calculateRatio()
	{
		//calculate the ratios that will keep us inside our box
		double yRatio = (getWidth() - 40) / (maximum.y - minimum.y);
		double xRatio = (getHeight() - 20) / (maximum.x - minimum.x);
		
		//which one is smallest?
		ratio = Math.min(yRatio, xRatio);
	}
	
	public void paint(Graphics g)
	{
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setPaint(Color.black);

	    g.setFont(new Font("SansSerif", Font.BOLD, 14));
	    g.setColor(Color.black);
		g.drawString("Layer at z: " + currentZ, 20, 20);
		
		Vector toolpaths = getLayerPaths(currentZ);
		Point3d start = new Point3d();
		Point3d end = new Point3d();
		
		//System.out.println("toolpaths:" + toolpaths.size());

		//draw our toolpaths.
		if (toolpaths.size() > 0)
		{
			for (Enumeration e = toolpaths.elements(); e.hasMoreElements();)
			{
				Vector path = (Vector)e.nextElement();
				//System.out.println("path points:" + path.size());

				if (path.size() > 1)
				{
					g.setColor(Color.black);
					start = (Point3d)path.firstElement();

					for (Enumeration e2 = path.elements(); e2.hasMoreElements();)
					{
						end = (Point3d)e2.nextElement();

						int startX = convertRealXToPointX(start.x);
						int startY = convertRealYToPointY(start.y);
						int endX = convertRealXToPointX(end.x);
						int endY = convertRealYToPointY(end.y);

						//System.out.println("line from: " + startX + ", " + startY + " to " + endX + ", " + endY);
						g.drawLine(startX, startY, endX, endY);

						start = new Point3d(end);
					}
				}
			}
		}
	}
	
	private Vector getLayerPaths(double layerZ)
	{
		Vector paths = new Vector();
		Vector path = new Vector();
		Point3d p;
		int i;
		
		for (Enumeration e = points.elements(); e.hasMoreElements();)
		{
			p = (Point3d)e.nextElement();
			
			//is this on our current layer?
			if (p.z == layerZ)
			{
				path.addElement(p);
				//System.out.println("added: " + p.toString());
			}
			//okay, not on layer... did we find a path?
			else if (path.size() > 0)
			{
				//System.out.println("added path of size " + path.size());
				paths.addElement(path);
				path = new Vector();
			}
		}
		
		//did we end on our current path?
		if (path.size() > 0)
			paths.addElement(path);
		
		return paths;
	}
	
	public int convertRealXToPointX(double x)
	{
		return (int)((x - minimum.x) * ratio) + 10;
	}
	
	public int convertRealYToPointY(double y)
	{
		// subtract from getheight to get a normal origin.
		return (getHeight() - (int)((y - minimum.y) * ratio));
	}
}