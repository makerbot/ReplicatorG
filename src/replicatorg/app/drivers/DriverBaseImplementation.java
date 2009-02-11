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

package replicatorg.app.drivers;

import javax.vecmath.Point3d;

import org.w3c.dom.Node;

import replicatorg.app.GCodeParser;
import replicatorg.app.exceptions.BuildFailureException;
import replicatorg.app.exceptions.GCodeException;
import replicatorg.app.models.MachineModel;

public class DriverBaseImplementation implements Driver
{
	// our gcode parser
	private GCodeParser parser;

	// command to parse
	private String command;

	//models for our machine
	protected MachineModel machine;

	//our firmware version info
	private String firmwareName = "Unknown";
	private int versionMajor = 0;
	private int versionMinor = 0;
	
	//our point offsets
	private Point3d[] offsets;

	//are we initialized?
	private boolean isInitialized = false;
	
	//the length of our last move.
	private double moveLength = 0.0;
	
	//our error variable.
	private String error = "";
	
	//how fast are we moving in mm/minute
	private double currentFeedrate;
	
	//what is our mode of positioning?
	private int positioningMode = 0;
	static public int ABSOLUTE = 0;
	static public int INCREMENTAL = 1;
	
	/**
	  * Creates the driver object.
	  */
	public DriverBaseImplementation()
	{
		//create our parser object
		parser = new GCodeParser();
		
		//initialize our offsets
		offsets = new Point3d[7];
		for (int i=0; i<7; i++)
			offsets[i] = new Point3d();
			
		//initialize our driver
		parser.init(this);
		
		//TODO: do this properly.
		machine = new MachineModel();
	}
	
	public void loadXML(Node xml)
	{
	}

	public void dispose()
	{
		System.out.println("Disposing of driver.");
		parser = null;
	}


	
	/*************************************************
	*  Initialization handling functions
	*************************************************/
	
	public void initialize()
	{
		setInitialized(true);
	}
	
	public void setInitialized(boolean status)
	{
		isInitialized = status;
	}
	
	public boolean isInitialized()
	{
		return isInitialized;
	}
	

	/*************************************************
	*  Error handling functions
	*************************************************/
	
	protected void setError(String e)
	{
		error = e;
	}
	
	public void checkErrors() throws BuildFailureException
	{
		if (error.length() > 0)
			throw new BuildFailureException(error);
	}

	/*************************************************
	*  Parser handling functions
	*************************************************/
	
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

	public void execute() throws GCodeException, InterruptedException
	{
	    assert (parser != null);
		parser.execute();
	}
	
	public boolean isFinished()
	{
		return true;
	}

	/*************************************************
	*  Firmware information functions
	*************************************************/
	
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
	
	/*************************************************
	*  Machine positioning functions
	*************************************************/
	
	public Point3d getOffset(int i)
	{
		return offsets[i];
	}
	
	public void setCurrentPosition(Point3d p)
	{
		machine.setCurrentPosition(p);
	}
	
	public Point3d getCurrentPosition()
	{
		return new Point3d(machine.getCurrentPosition());
	}
	
	public Point3d getPosition()
	{
	  return getCurrentPosition();
	}
	
	public void queuePoint(Point3d p)
	{
		Point3d currentPosition = machine.getCurrentPosition();
		
		//calculate the length of each axis move
		double xFactor = Math.pow(p.x - currentPosition.x, 2);
		double yFactor = Math.pow(p.y - currentPosition.y, 2);
		double zFactor = Math.pow(p.z - currentPosition.z, 2);

		//add to the total length
		moveLength += Math.sqrt(xFactor + yFactor + zFactor);

		//save it as our current position now.
		machine.setCurrentPosition(p);
	}
	
	public double getMoveLength()
	{
		return moveLength;
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
	
	/*************************************************
	*  various homing functions
	*************************************************/
	public void homeXYZ()
	{
		machine.setCurrentPosition(new Point3d());
	}
	
	public void homeXY()
	{
		Point3d temp = machine.getCurrentPosition();
		
		temp.x = 0;
		temp.y = 0;
		
		machine.setCurrentPosition(temp);
	}
	
	public void homeX()
	{
		Point3d temp = machine.getCurrentPosition();
		
		temp.x = 0;
		
		machine.setCurrentPosition(temp);
	}
	
	public void homeY()
	{
		Point3d temp = machine.getCurrentPosition();
		
		temp.y = 0;
		
		machine.setCurrentPosition(temp);
	}
	
	public void homeZ()
	{
		Point3d temp = machine.getCurrentPosition();
		
		temp.z = 0;
		
		machine.setCurrentPosition(temp);
	}	

	/*************************************************
	*  Machine interface functions
	*************************************************/
	public MachineModel getMachine()
	{
		return machine;
	}
	
	public void setMachine(MachineModel m)
	{
		machine = m;
	}

	/*************************************************
	*  Tool interface functions
	*************************************************/
	public void requestToolChange(int toolIndex)
	{
		machine.selectTool(toolIndex);
	}
	
	public void selectTool(int toolIndex)
	{
		machine.selectTool(toolIndex);
	}
	
	/*************************************
	*  pause function
	*************************************/
	public void delay(long millis)
	{
		//System.out.println("Delay: " + millis);
	}
	
	/*************************************
	*  functions for dealing with clamps
	*************************************/
	public void openClamp(int index)
	{
		machine.getClamp(index).open();
	}
	
	public void closeClamp(int index)
	{
		machine.getClamp(index).close();
	}
	
	/*************************************
	*  enabling/disabling our drivers (steppers, servos, etc.)
	*************************************/
	public void enableDrives()
	{
		machine.enableDrives();
	}
	
	public void disableDrives()
	{
		machine.disableDrives();
	}
	
	/*************************************
	*  Change our gear ratio.
	*************************************/

	public void changeGearRatio(int ratioIndex)
	{
		machine.changeGearRatio(ratioIndex);
	}
	
	/*****************************************************************************
	*  toolhead interface commands
	*****************************************************************************/

	/*************************************
	*  Motor interface functions
	*************************************/
 	public void setMotorDirection(int dir)
	{
		machine.currentTool().setMotorDirection(dir);
	}
	
	public void setMotorRPM(double rpm)
	{
		machine.currentTool().setMotorSpeedRPM(rpm);
	}

	public void setMotorSpeedPWM(int pwm)
	{
		machine.currentTool().setMotorSpeedPWM(pwm);
	}
	
	public void enableMotor()
	{
		machine.currentTool().enableMotor();
	}
	
	public void disableMotor()
	{
		machine.currentTool().disableMotor();
	}
	
	public double getMotorRPM()
	{
		return machine.currentTool().getMotorSpeedReadingRPM();
	}

	public int getMotorSpeedPWM()
	{
		return machine.currentTool().getMotorSpeedReadingPWM();
	}

	
	/*************************************
	*  Spindle interface functions
	*************************************/
	public void setSpindleDirection(int dir)
	{
		machine.currentTool().setSpindleDirection(dir);
	}

	public void setSpindleRPM(double rpm)
	{
		machine.currentTool().setSpindleSpeedRPM(rpm);
	}

	public void setSpindleSpeedPWM(int pwm)
	{
		machine.currentTool().setSpindleSpeedPWM(pwm);
	}
	
	public void enableSpindle()
	{
		machine.currentTool().enableSpindle();
	}
	
	public void disableSpindle()
	{
		machine.currentTool().disableSpindle();
	}
	
	public double getSpindleRPM()
	{
		return machine.currentTool().getSpindleSpeedReadingRPM();
	}

	public int getSpindleSpeedPWM()
	{
		return machine.currentTool().getSpindleSpeedReadingPWM();
	}
	
	/*************************************
	*  Temperature interface functions
	*************************************/
	public void setTemperature(double temperature)
	{
		machine.currentTool().setTargetTemperature(temperature);
	}

	public void readTemperature()
	{
		
	}

	public double getTemperature()
	{
		readTemperature();
		
		return machine.currentTool().getCurrentTemperature();
	}

	/*************************************
	*  Flood Coolant interface functions
	*************************************/
	public void enableFloodCoolant()
	{
		machine.currentTool().enableFloodCoolant();
	}
	
	public void disableFloodCoolant()
	{
		machine.currentTool().disableFloodCoolant();
	}

	/*************************************
	*  Mist Coolant interface functions
	*************************************/
	public void enableMistCoolant()
	{
		machine.currentTool().enableMistCoolant();
	}
	
	public void disableMistCoolant()
	{
		machine.currentTool().disableMistCoolant();
	}

	/*************************************
	*  Fan interface functions
	*************************************/
	public void enableFan()
	{
		machine.currentTool().enableFan();
	}
	
	public void disableFan()
	{
		machine.currentTool().disableFan();
	}
	
	/*************************************
	*  Valve interface functions
	*************************************/
	public void openValve()
	{
		machine.currentTool().openValve();
	}
	
	public void closeValve()
	{
		machine.currentTool().closeValve();
	}
	
	/*************************************
	*  Collet interface functions
	*************************************/
	public void openCollet()
	{
		machine.currentTool().openCollet();
	}
	
	public void closeCollet()
	{
		machine.currentTool().closeCollet();
	}
}
