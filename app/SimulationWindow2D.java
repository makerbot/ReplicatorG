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
import java.awt.image.*;
import javax.swing.*;
import java.util.*;
import javax.vecmath.*;

public class SimulationWindow2D extends SimulationWindow
{	
	private Point3d minimum;
	private Point3d maximum;
	private double currentZ;
	
	private double ratio = 1.0;

	private Vector points;
	
	private long lastRepaint = 0;
	
	public SimulationWindow2D ()
	{
		super();

		this.setVisible(true);

		//setup our rendering/buffer strategy
		createBufferStrategy(2);
		
		setTitle("2D Build Simulation");
		
		//init our bounds.
		minimum = new Point3d();
		maximum = new Point3d();
		currentZ = 0.0;
		
		//initialize our vector
		points = new Vector();
		
		//start us off at 0,0,0
		queuePoint(new Point3d());
		
		setBackground(Color.white);
		setForeground(Color.white);
	}
	
	synchronized public void queuePoint(Point3d point)
	{
		//System.out.println("queued: " + point.toString());

		if (points.size() == 0)
		{
			minimum = new Point3d(point);
			maximum = new Point3d(point);
		}
		else
		{
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
		}
			
		Point3d myPoint = new Point3d(point);
		points.addElement(myPoint);
		
		currentZ = point.z;

		calculateRatio();
	
		doRender();
	}

	public void calculateRatio()
	{
		//calculate the ratios that will keep us inside our box
		double yRatio = (getWidth() - 40) / (maximum.y - minimum.y);
		double xRatio = (getHeight() - 20) / (maximum.x - minimum.x);
		
		//which one is smallest?
		ratio = Math.min(yRatio, xRatio);
	}
	
/*
	public void repaint(Graphics g)
	{
		super
*/
	
	public void doRender()
	{
		BufferStrategy myStrategy = getBufferStrategy();
		Graphics g = myStrategy.getDrawGraphics();
		
		//clear it
		g.setColor(Color.white);
		Rectangle bounds = g.getClipBounds();
		g.fillRect(0, 0, getWidth(), getHeight());

		//init some prefs
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setPaint(Color.black);

		//draw some helper text.
	    g.setFont(new Font("SansSerif", Font.BOLD, 14));
	    g.setColor(Color.black);
		g.drawString("Layer at z: " + currentZ, 20, 20);

		//TODO: add/fix scale indicators
		//drawScaleIndicators(g);
		//drawToolpaths(g);
		drawLastPoints(g);

		myStrategy.show();
	}
	
	private void drawScaleIndicators(Graphics g)
	{
		int xIncrements = 9;
		int yIncrements = 9;
		int xSpacing = 2;
		int ySpacing = 3;
		
		double xIncrement = (maximum.x - minimum.x - xSpacing) / xIncrements;
		double yIncrement = (maximum.y - minimum.y - ySpacing) / yIncrements;
		
		//draw the main bars.
		g.drawLine(xSpacing, ySpacing, xSpacing, getHeight()-ySpacing);
		g.drawLine(xSpacing, getHeight()-ySpacing, getWidth()-xSpacing, getHeight()-ySpacing);
		
		//draw our x ticks
		for (int i=1; i<=xIncrements+1; i++)
		{
			double xReal = i * xIncrement;
			int xPoint = convertRealXToPointX(xReal);
			
			g.drawLine(xPoint, getHeight()-ySpacing, xPoint, getHeight()-ySpacing-10);
		}
		
		//draw our y ticks
		for (int i=1; i<yIncrements; i++)
		{
			double yReal = i * yIncrement;
			int yPoint = convertRealYToPointY(yReal);
			
			g.drawLine(ySpacing, getHeight()-ySpacing-yPoint, ySpacing+10, getHeight()-ySpacing-yPoint);
		}
		
	}

	private void drawLastPoints(Graphics g)
	{
		Color aboveColor = new Color(255, 0, 0);
		Color currentColor = new Color(0, 255, 0);
		Color belowColor = new Color(0, 0, 255);

		java.util.List lastPoints = getLastPoints(1000);
		
		Point3d start;
		Point3d end;
		
		double belowZ = currentZ;
		double aboveZ = currentZ;

		//color coding.
		int aboveTotal = 0;
		int belowTotal = 0;
		int currentTotal = 0;
		int aboveCount = 0;
		int belowCount = 0;
		int currentCount = 0;
		
		//draw our toolpaths.
		if (lastPoints.size() > 0)
		{
			start = (Point3d)lastPoints.get(0);

			//start from the most recent line backwards to find the above/below layers.
			for (int i=lastPoints.size()-1; i>=0; i--)
			{
				end = (Point3d)lastPoints.get(i);
				
				if (!start.equals(end))
				{
					//line below current plane
					if (end.z < currentZ)
					{
						//we only want one layer up/down
						if (end.z < belowZ && belowZ != currentZ)
							continue;
							
						belowZ = end.z;
						belowTotal++;
					}				
					//line above current plane
					else if (end.z > currentZ)
					{
						//we only want one layer up/down
						if (end.z > aboveZ && aboveZ != currentZ)
							continue;
						
						aboveZ = end.z;
						aboveTotal++;
					}
					//current line.
					else if (end.z == currentZ)
					{
						currentTotal++;
					}
					else
						continue;

					start = new Point3d(end);
				}
			}
			
			//draw all our lines now!
			for (ListIterator li = lastPoints.listIterator(); li.hasNext();)
			{
				end = (Point3d)li.next();

				//we have to move somewhere!
				if (!start.equals(end))
				{
					int startX = convertRealXToPointX(start.x);
					int startY = convertRealYToPointY(start.y);
					int endX = convertRealXToPointX(end.x);
					int endY = convertRealYToPointY(end.y);
					int colorValue;
					
					//line below current plane
					if (end.z < currentZ && end.z >= belowZ)
					{
						belowCount++;
						
						colorValue = 255-3*(belowTotal-belowCount);
						colorValue = Math.max(0, colorValue);
						colorValue = Math.min(255, colorValue);

						belowColor = new Color(0, 0, colorValue);
						g.setColor(belowColor);
					}
					//line above current plane
					if (end.z > currentZ && end.z <= aboveZ)
					{
						aboveCount++;

						colorValue = 255-3*(aboveTotal-aboveCount);
						colorValue = Math.max(0, colorValue);
						colorValue = Math.min(255, colorValue);

						aboveColor = new Color(colorValue, 0, 0);
						g.setColor(aboveColor);
					}
					//line in current plane
					else if (end.z == currentZ)
					{
						currentCount++;

						colorValue = 255-3*(currentTotal-currentCount);
						colorValue = Math.max(0, colorValue);
						colorValue = Math.min(255, colorValue);

						currentColor = new Color(0, colorValue, 0);
						g.setColor(currentColor);
					}
					//bail, your'e not on our plane.
					else
						continue;

					//draw up arrow
					if (end.z > start.z)
					{
						g.setColor(Color.red);
						g.drawOval(startX-5, startY-5, 10, 10);
						g.drawLine(startX-5, startY, startX+5, startY);
						g.drawLine(startX, startY-5, startX, startY+5);
					}
					//draw down arrow
					else if (end.z < start.z)
					{
						g.setColor(Color.blue);
						g.drawOval(startX-5, startY-5, 10, 10);
						g.drawOval(startX-1, startY-1, 2, 2);
					}
					//normal XY line - only draw lines on current layer or above.
					else if (end.z >= currentZ)
					{
						g.drawLine(startX, startY, endX, endY);
					}

					start = new Point3d(end);
				}
			}
			
			/*
			System.out.println("counts:");
			System.out.println(belowCount + " / " + belowTotal);
			System.out.println(aboveCount + " / " + aboveTotal);
			System.out.println(currentCount + " / " + currentTotal);
			*/
		}
	}

	private java.util.List getLastPoints(int count)
	{
		int index = Math.max(0, points.size()-count);		

		java.util.List mypoints = points.subList(index, points.size());
		
		return mypoints;
	}
	
	private void drawToolpaths(Graphics g)
	{
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
