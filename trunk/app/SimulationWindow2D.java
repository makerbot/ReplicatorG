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

		/*
		System.out.println("current queue:");
		for (Enumeration e = points.elements(); e.hasMoreElements();)
		{
			Point3d p = (Point3d)e.nextElement();
			System.out.println(p.toString());
		}
		*/

//		resizeIfNeeded();
			
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
		g.drawString("Points:" + points.size(), 20, 40);

		g.drawLine(40, 40, 200, 200);
		g.drawLine(200, 40, 40, 200);
		
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

						System.out.println("line from: " + startX + ", " + startY + " to " + endX + ", " + endY);
						g.drawLine(startX, startY, endX, endY);

						start = new Point3d(end);
					}
				}
			}
		}

/*
		//draw our toolpaths.
		if (toolpaths.size() > 0)
		{
			for (int i=0; i<toolpaths.size(); i++)
			{
				Vector path = (Vector)toolpaths.get(i);
				//System.out.println("path points:" + path.size());

				if (path.size() > 1)
				{
					g.setColor(Color.black);
					start = (Point3d)path.get(0);
					for (int j=1; j<path.size(); j++)
					{
						end = (Point3d)path.get(j);

						int startX = convertRealXToPointX(start.x);
						int startY = convertRealYToPointY(start.y);
						int endX = convertRealXToPointX(end.x);
						int endY = convertRealYToPointY(end.y);

						System.out.println("line from: " + startX + ", " + startY + " to " + endX + ", " + endY);
						g.drawLine(startX, startY, endX, endY);

						start = end;
					}
				}
			}
		}
*/
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
				System.out.println("added: " + p.toString());
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
		return (int)(x * 10);
//		return (int)Math.round(getWidth() * (Math.abs(minimum.x - x) / Math.abs(maximum.x - minimum.x)));
	}
	
	public int convertRealYToPointY(double y)
	{
		return (int)(y * 10);
//		return (int)Math.round(getWidth() * (Math.abs(minimum.y - y) / Math.abs(maximum.y - minimum.y)));
	}
}