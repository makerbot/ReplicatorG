/*
  DriverBaseImplementation.java

  A basic driver implementation to build from.

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

package processing.app.drivers;

import processing.app.exceptions.*;
import processing.app.*;

import java.util.regex.*;
import javax.vecmath.*;
import org.w3c.dom.*;

public class DriverBaseImplementation implements Driver
{
	// our gcode parser
	protected GCodeParser parser;

	// command to parse
	private String command;

	//our tool drivers
	private ToolDriver[] tools;
	private ToolDriver currentTool;

	//our current position
	private Point3d currentPosition;
	private double currentFeedrate;
	
	//our versions
	private int versionMajor = 0;
	private int versionMinor = 0;
	
	//our offsets
	protected Point3d[] offsets;

	//are we initialized?
	protected boolean isInitialized = false;

	/**
	  * Creates the driver object.
	  */
	public DriverBaseImplementation()
	{
		parser = new GCodeParser();
		
		currentPosition = new Point3d();
		currentFeedrate = 0.0;

		//todo: change to loadToolDrivers();
		currentTool = new ToolDriver();
		
		//initialize our offsets
		offsets = new Point3d[7];
		for (int i=0; i<7; i++)
			offsets[i] = new Point3d();
			
		//initialize our driver
		parser.init(this);
	}
	
	public void loadXML(Node xml)
	{
		//TODO: load standard driver configs.
	}
	
	public void initialize()
	{
		isInitialized = true;
	}
	
	public boolean isInitialized()
	{
		return isInitialized;
	} 

	public void parse(String cmd)
	{
		command = cmd;
		parser.parse(cmd);
	}
	
	public boolean isFinished()
	{
		return true;
	}
	
	public void dispose()
	{
		parser = null;
	}

	public void handleStops() throws JobRewindException, JobEndException, JobCancelledException
	{
		parser.handleStops();
	}
	
	public void execute() throws GCodeException
	{
		parser.execute();
	}
	
	public String getVersion()
	{
		return Integer.toString(versionMajor) + "." + Integer.toString(versionMinor);
	}
	
	public Point3d getOffset(int i)
	{
		return offsets[i];
	}
	
	public void setCurrentPosition(Point3d p)
	{
		currentPosition = new Point3d(p);
	}
	
	public Point3d getCurrentPosition()
	{
		return new Point3d(currentPosition);
	}
	
	public void queuePoint(Point3d p)
	{
		currentPosition = new Point3d(p);
	}


	/**
	* Tool methods
	*/
	public void loadToolDrivers(Node n)
	{
		
	}
	
	public void requestToolChange(int toolIndex)
	{
		
	}
	
	public void selectTool(int toolIndex)
	{
		currentTool = tools[toolIndex];
	}

	public ToolDriver currentTool()
	{
		return currentTool;
	}
	
	/**
	* sets the feedrate in mm/minute
	*/
	public void setFeedrate(double feed)
	{
		currentFeedrate = feed;
	}
	
	/**
	* sets the feedrate in mm/minute
	*/
	public double getCurrentFeedrate()
	{
		return currentFeedrate;
	}
	
	
	/**
	* various homing functions
	*/
	public void homeXYZ() {}
	public void homeXY() {}
	public void homeX() {}
	public void homeY() {}
	public void homeZ() {}	
	
	/**
	* delay / pause function
	*/
	public void delay(long millis)
	{
		//System.out.println("Delay: " + millis);
	}
	
	/**
	* functions for dealing with clamps
	*/
	public void openClamp(int clampIndex) {}
	public void closeClamp(int clampIndex) {}
	
	/**
	* enabling/disabling our drivers (steppers, servos, etc.)
	*/
	public void enableDrives() {}
	public void disableDrives() {}
	
	/**
	* change our gear ratio
	*/
	public void changeGearRatio(int ratioIndex) {}
	
}
