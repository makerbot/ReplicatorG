/*
  Driver.java

  Provides an interface for driving various machines.

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

package processing.app;
import processing.app.drivers.*;
import processing.app.exceptions.*;

import java.util.regex.*;
import javax.vecmath.*;

import org.w3c.dom.*;

//import org.xml.sax.*;
//import org.xml.sax.helpers.XMLReaderFactory;

public interface Driver
{
	/**
	* High level functions
	*/
	
	/**
	* parse and load configuration data from XML
	*/
	public void loadXML(Node xml);
	
	/**
	* parse a command.  usually passes it through to the parser.
	*/
	public void parse(String cmd);
	
	/**
	* get our parser object
	*/
	public GCodeParser getParser();
	
	/**
	* are we finished with the last command?
	*/
	public boolean isFinished();
	
	/**
	* setup our driver for use.
	**/
	public void initialize();

	/**
	* clean up the driver
	*/
	public void dispose();

	/**
	* execute the recently parsed GCode
	*/
	public void execute() throws GCodeException;

	/**
	* get version information from the driver
	*/
	public String getVersion();
	
	/**
	* Positioning Methods
	**/
	public void setCurrentPosition(Point3d p);
	public Point3d getCurrentPosition();
	public void queuePoint(Point3d p);
	public Point3d getOffset(int i);


	/**
	* Tool methods
	*/
	public void loadToolDrivers(Node n);
	public void requestToolChange(int toolIndex);
	public void selectTool(int toolIndex);
	public ToolDriver currentTool();
	
	/**
	* sets the feedrate in mm/minute
	*/
	public void setFeedrate(double feed);
	
	/**
	* sets the feedrate in mm/minute
	*/
	public double getCurrentFeedrate();
	
	
	/**
	* various homing functions
	*/
	public void homeXYZ();
	public void homeXY();
	public void homeX();
	public void homeY();
	public void homeZ();	
	
	/**
	* delay / pause function
	*/
	public void delay(long millis);
	
	/**
	* functions for dealing with clamps
	*/
	public void openClamp(int clampIndex);
	public void closeClamp(int clampIndex);
	
	/**
	* enabling/disabling our drivers (steppers, servos, etc.)
	*/
	public void enableDrives();
	public void disableDrives();
	
	/**
	* change our gear ratio
	*/
	public void changeGearRatio(int ratioIndex);


	/*************************************
	*  Motor interface functions
	*************************************/
 	public void setMotorDirection(int dir);
	public void setMotorSpeed(double rpm);
	public void enableMotor();
	public void disableMotor();
	public double getMotorSpeed();

	/*************************************
	*  Spindle interface functions
	*************************************/
	public void setSpindleDirection(int dir);
	public void setSpindleSpeed(double rpm);
	public void enableSpindle();
	public void disableSpindle();
	public double getSpindleSpeed();
	
	/*************************************
	*  Temperature interface functions
	*************************************/
	public void setTemperature(double temperature);
	public void readTemperature();
	public double getTemperature();

	/*************************************
	*  Flood Coolant interface functions
	*************************************/
	public void enableFloodCoolant();
	public void disableFloodCoolant();

	/*************************************
	*  Mist Coolant interface functions
	*************************************/
	public void enableMistCoolant();
	public void disableMistCoolant();

	/*************************************
	*  Fan interface functions
	*************************************/
	public void enableFan();
	public void disableFan();
	
	/*************************************
	*  Valve interface functions
	*************************************/
	public void openValve();
	public void closeValve();
	
	/*************************************
	*  Collet interface functions
	*************************************/
	public void openCollet();
	public void closeCollet();
}
