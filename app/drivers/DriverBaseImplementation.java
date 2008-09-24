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
	protected ToolModel[] tools;
	protected ToolModel currentTool;

	//our current position
	//todo: move these to MachineModel
	private Point3d currentPosition;
	private double currentFeedrate;
	
	//our versions
	String firmwareName = "Unknown";
	private int versionMajor = 0;
	private int versionMinor = 0;
	
	//our offsets
	protected Point3d[] offsets;

	//are we initialized?
	protected boolean isInitialized = false;
	
	//the length of our last move.
	private double moveLength = 0.0;
	
	//our error variable.
	private String error = "";
	
	/**
	  * Creates the driver object.
	  */
	public DriverBaseImplementation()
	{
		parser = new GCodeParser();
		
		currentPosition = new Point3d();
		currentFeedrate = 0.0;

		//todo: change to loadTools()
		currentTool = new ToolModel();
		
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

		loadTools(xml);
	}
	
	protected void loadTools(Node xml)
	{
		//todo: load from XML
	}
	
	public void initialize()
	{
		isInitialized = true;
	}
	
	public boolean isInitialized()
	{
		return isInitialized;
	}
	
	protected void setError(String e)
	{
		error = e;
	}
	
	public void checkErrors() throws BuildFailureException
	{
		if (error.length() > 0)
			throw new BuildFailureException(error);
	}

	public void parse(String cmd)
	{
		//reset our values.
		moveLength = 0.0;
		
		command = cmd;
		parser.parse(cmd);
	}
	
	public GCodeParser getParser()
	{
		return parser;
	}
	
	public boolean isFinished()
	{
		return true;
	}
	
	public void dispose()
	{
		parser = null;
	}

	public void execute() throws GCodeException
	{
		parser.execute();
	}
	
	public String getFirmwareInfo()
	{
		return firmwareName + " v" + getVersion(); 
	}
	
	public String getVersion()
	{
		return Integer.toString(versionMajor) + "." + Integer.toString(versionMinor);
	}
	
	public int getMajorVersion()
	{
		return versionMajor;
	}
	
	public int getMinorVersion()
	{
		return versionMinor;
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
		//calculate the length of each axis move
		double xFactor = Math.pow(p.x - currentPosition.x, 2);
		double yFactor = Math.pow(p.y - currentPosition.y, 2);
		double zFactor = Math.pow(p.z - currentPosition.z, 2);

		//add to the total length
		moveLength += Math.sqrt(xFactor + yFactor + zFactor);

		//save it as our current position now.
		currentPosition = new Point3d(p);
	}
	
	public double getMoveLength()
	{
		return moveLength;
	}


	/**
	* Tool methods
	*/
	public void requestToolChange(int toolIndex)
	{
		selectTool(toolIndex);
	}
	
	public void selectTool(int toolIndex)
	{
		currentTool = tools[toolIndex];
	}

	public ToolModel currentTool()
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
	
	/*****************************************************************************
	*  toolhead interface commands
	*****************************************************************************/

	/*************************************
	*  Motor interface functions
	*************************************/
 	public void setMotorDirection(int dir)
	{
		currentTool().setMotorDirection(dir);
	}
	
	public void setMotorSpeed(double rpm)
	{
		currentTool().setMotorSpeed(rpm);
	}
	
	public void enableMotor()
	{
		currentTool().enableMotor();
	}
	
	public void disableMotor()
	{
		currentTool().disableMotor();
	}
	
	public void readMotorSpeed()
	{
		
	}
	
	public double getMotorSpeed()
	{
		return currentTool().getMotorSpeed();
	}

	/*************************************
	*  Spindle interface functions
	*************************************/
	public void setSpindleDirection(int dir)
	{
		currentTool().setSpindleDirection(dir);
	}

	public void setSpindleSpeed(double rpm)
	{
		currentTool().setSpindleSpeed(rpm);
	}
	
	public void enableSpindle()
	{
		currentTool().enableSpindle();
	}
	
	public void disableSpindle()
	{
		currentTool.disableSpindle();
	}
	
	public void readSpindleSpeed()
	{
		
	}
	
	public double getSpindleSpeed()
	{ 
		return currentTool.getSpindleSpeed();
	}
	
	/*************************************
	*  Temperature interface functions
	*************************************/
	public void setTemperature(double temperature)
	{
		currentTool().setTargetTemperature(temperature);
	}

	public void readTemperature()
	{
		
	}

	public double getTemperature()
	{
		readTemperature();
		
		return currentTool.getCurrentTemperature();
	}

	/*************************************
	*  Flood Coolant interface functions
	*************************************/
	public void enableFloodCoolant()
	{
		currentTool().enableFloodCoolant();
	}
	
	public void disableFloodCoolant()
	{
		currentTool().disableFloodCoolant();
	}

	/*************************************
	*  Mist Coolant interface functions
	*************************************/
	public void enableMistCoolant()
	{
		currentTool().enableMistCoolant();
	}
	
	public void disableMistCoolant()
	{
		currentTool().disableMistCoolant();
	}

	/*************************************
	*  Fan interface functions
	*************************************/
	public void enableFan()
	{
		currentTool().enableFan();
	}
	
	public void disableFan()
	{
		currentTool().disableFan();
	}
	
	/*************************************
	*  Valve interface functions
	*************************************/
	public void openValve()
	{
		currentTool().openValve();
	}
	
	public void closeValve()
	{
		currentTool().closeValve();
	}
	
	/*************************************
	*  Collet interface functions
	*************************************/
	public void openCollet()
	{
		currentTool().openCollet();
	}
	
	public void closeCollet()
	{
		currentTool().closeCollet();
	}
}
