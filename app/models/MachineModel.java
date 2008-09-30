/*
  MachineModel.java

  A class to model a 3-axis machine.

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

package processing.app.models;

import processing.app.*;
import processing.app.tools.*;

import org.w3c.dom.*;
import javax.vecmath.*;
import java.util.*;

public class MachineModel
{
	//our xml config info
	protected Node xml;
	
	//our machine space
	private Point3d currentPosition;
	private Point3d minimum;
	private Point3d maximum;

	//feedrate information
	private Point3d maximumFeedrates;
	private Point3d stepsPerMM;
	
	//our drive status
	protected boolean drivesEnabled = true;
	protected int gearRatio = 0;
	
	//our tool models
	protected Vector tools;
	protected ToolModel currentTool;

	//our clamp models	
	protected Vector clamps;

	/*************************************
	*  Creates the model object.
	*************************************/
	public MachineModel()
	{
		clamps = new Vector();
		tools = new Vector();
		
		currentPosition = new Point3d();
		minimum = new Point3d();
		maximum = new Point3d();
		maximumFeedrates = new Point3d();
		stepsPerMM = new Point3d(1, 1, 1); //use ones, because we divide by this!
	}
	
	//load data from xml config
	public void loadXML(Node node)
	{
		xml = node;
		
		parseAxes();
		parseClamps();
		parseTools();
	}
	
	//load axes configuration
	private void parseAxes()
	{
		if(XML.hasChildNode(xml, "geometry"))
		{
			Node geometry = XML.getChildNodeByName(xml, "geometry");
			
			//look through the axes.
			NodeList axes = geometry.getChildNodes();
			for (int i=0; i<axes.getLength(); i++)
			{
				Node axis = axes.item(i);
				
				if (axis.getNodeName().equals("axis"))
				{
					//parse our information.
					String id = XML.getAttributeValue(axis, "id");

					//initialize values
				 	double length = 0.0;
				 	double maxFeedrate = 0.0;
				 	double scale = 1.0;
					
					//if values are missing, ignore them.
					try {
					 	length = Double.parseDouble(XML.getAttributeValue(axis, "length"));
					 	maxFeedrate = Double.parseDouble(XML.getAttributeValue(axis, "maxfeedrate"));
					 	scale = Double.parseDouble(XML.getAttributeValue(axis, "scale"));
					} catch (Exception e) {}
					
					//create the right variables.
					if (id.toLowerCase().equals("x"))
					{
						maximum.x = length;
						maximumFeedrates.x = maxFeedrate;
						stepsPerMM.x = scale;
					}
					else if (id.toLowerCase().equals("y"))
					{
						maximum.y = length;
						maximumFeedrates.y = maxFeedrate;
						stepsPerMM.y = scale;
					}
					else if (id.toLowerCase().equals("z"))
					{
						maximum.z = length;
						maximumFeedrates.z = maxFeedrate;
						stepsPerMM.z = scale;
					}

					System.out.println("Loading axis " + id + ": (Length: " + length + "mm, max feedrate: " + maxFeedrate + " mm/min, scale: " + scale + " steps/mm)");
				}
			}
		}
	}
	
	//load clamp configuration
	private void parseClamps()
	{
		if(XML.hasChildNode(xml, "clamps"))
		{
			Node clampsNode = XML.getChildNodeByName(xml, "clamps");
			
			//look through the axes.
			NodeList clampKids = clampsNode.getChildNodes();
			for (int i=0; i<clampKids.getLength(); i++)
			{
				Node clampNode = clampKids.item(i);
				
				ClampModel clamp = new ClampModel(clampNode);
				clamps.add(clamp);
				
				System.out.println("adding clamp #" + clamps.size());
			}
		}
	}
	
	//load tool configuration
	private void parseTools()
	{
		if(XML.hasChildNode(xml, "tools"))
		{
			Node toolsNode = XML.getChildNodeByName(xml, "tools");
			
			//look through the axes.
			NodeList toolKids = toolsNode.getChildNodes();
			for (int i=0; i<toolKids.getLength(); i++)
			{
				Node toolNode = toolKids.item(i);
				
				if (toolNode.getNodeName().equals("tool"))
				{
					ToolModel tool = new ToolModel(toolNode);
					tool.setIndex(tools.size());
					tools.add(tool);
				}
			}
		}
	}

	/*************************************
	* Basic positioning information
	*************************************/
	public Point3d getCurrentPosition()
	{
		return new Point3d(currentPosition);
	}
	
	public void setCurrentPosition(Point3d p)
	{
		currentPosition = new Point3d(p);
	}

	/*************************************
	*  Convert steps to millimeter units
	*************************************/
	public double xStepsToMM(long steps)
	{
		return steps/stepsPerMM.x;
	}
	
	public double yStepsToMM(long steps)
	{
		return steps/stepsPerMM.y;
	}
	
	public double zStepsToMM(long steps)
	{
		return steps/stepsPerMM.z;
	}
	
	public Point3d stepsToMM(Point3d steps)
	{
		Point3d temp = new Point3d();

		temp.x = steps.x/stepsPerMM.x;
		temp.y = steps.y/stepsPerMM.y;
		temp.z = steps.z/stepsPerMM.z;
		
		return temp;
	}
	
	/*************************************
	*  Convert millimeters to machine steps
	*************************************/
	public long xMMtoSteps(double mm)
	{
		return Math.round(mm * stepsPerMM.x);
	}
	
	public long yMMtoSteps(double mm)
	{
		return Math.round(mm * stepsPerMM.y);
	}
	
	public long zMMtoSteps(double mm)
	{
		return Math.round(mm * stepsPerMM.z);
	}

	public Point3d mmToSteps(Point3d mm)
	{
		Point3d temp = new Point3d();

		temp.x = Math.round(mm.x * stepsPerMM.x);
		temp.y = Math.round(mm.y * stepsPerMM.y);
		temp.z = Math.round(mm.z * stepsPerMM.z);
		
		return temp;
	}

	/*************************************
	* Drive interface functions
	*************************************/
	public void enableDrives()
	{
		drivesEnabled = true;
	}
	
	public void disableDrives()
	{
		drivesEnabled = false;
	}
	
	public boolean areDrivesEnabled()
	{
		return drivesEnabled;
	}
	
	/*************************************
	* Gear Ratio functions
	*************************************/
	public void changeGearRatio(int ratioIndex)
	{
		gearRatio = ratioIndex;
	}
	
	/*************************************
	* Clamp interface functions
	*************************************/
	public ClampModel getClamp(int index)
	{
		try {
			ClampModel c = (ClampModel)clamps.get(index);
			return c;
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Cannot get non-existant clamp (#" + index + ".");
			e.printStackTrace();
		}
		
		return null;
	}
	
	/*************************************
	*  Tool interface functions
	*************************************/
	public void selectTool(int index)
	{
		try {
			currentTool = (ToolModel)tools.get(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Cannot select non-existant tool (#" + index + ".");
			e.printStackTrace();
		}
	}

	public ToolModel currentTool()
	{
		return currentTool;
	}
	
	public ToolModel getTool(int index)
	{
		try {
			ToolModel t = (ToolModel)tools.get(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Cannot get non-existant tool (#" + index + ".");
			e.printStackTrace();
		}
		
		return null;
	}
	
	public Vector getTools()
	{
		return tools;
	}

	public void addTool(ToolModel t)
	{
		tools.add(t);
	}
	
	
	public void setTool(int index, ToolModel t)
	{
		try {
			tools.set(index, t);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Cannot set non-existant tool (#" + index + ".");
			e.printStackTrace();
		}
	}

}
