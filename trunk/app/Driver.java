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

import java.util.regex.*;
import javax.vecmath.*;

import org.w3c.dom.*;

//import org.xml.sax.*;
//import org.xml.sax.helpers.XMLReaderFactory;

public class Driver
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

	/**
	  * Creates the driver object.
	  */
	public Driver()
	{
		parser = new GCodeParser();
		
		currentPosition = new Point3d();
		currentFeedrate = 0.0;

		//todo: change to loadToolDrivers();
		currentTool = new ToolDriver();
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
		parser.execute(this);
	}
	
	public String getVersion()
	{
		return Integer.toString(versionMajor) + "." + Integer.toString(versionMinor);
	}
	
	/**
	 * Create and instantiate the driver class for our particular machine
	 * @param String name the name of the driver to instantiate
	 * @return Driver a driver object ready for parsing / running gcode
	 */
	public static Driver factory(Node xml)
	{
		//find the "name" attribute first
		if (xml.hasAttributes())
		{
			NamedNodeMap map = xml.getAttributes();
			Node attribute = map.getNamedItem("name");
			if (attribute != null)
			{
				String driverName = attribute.getNodeValue().trim();

				//use our common factory
				return factory(driverName, xml);
			}
		}

		//fail over to "name" element
		if (xml.hasChildNodes())
		{
			NodeList kids = xml.getChildNodes();
			for (int j=0; j<kids.getLength(); j++)
			{
				Node kid = kids.item(j);

				if (kid.getNodeName().equals("name"))
				{
					String driverName = kid.getFirstChild().getNodeValue().trim();

					//use our common factory
					return factory(driverName, xml);
				}
			}
		}

		System.out.println("Failing over to null driver.");
		
		//bail with a fake driver.
		return new NullDriver();
	}
	
	//common driver factory.
	public static Driver factory (String driverName, Node xml)
	{
		System.out.println("Loading driver: " + driverName);
		
		if (driverName.equals("serialpassthrough"))
			return new SerialPassthroughDriver(xml);
		else
		{
			System.out.println("Driver not found, failing over to 'null'.");
			return new NullDriver(xml);
		}
	}
	
	/**
	* empty parameters?  give up a null driver.
	*/
	public static Driver factory()
	{
		return new NullDriver();
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
	public void delay(long millis) {}
	
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

class JobCancelledException extends Exception
{
}

class JobEndException extends Exception
{
}

class JobRewindException extends Exception
{
}
