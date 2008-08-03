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
	// command to parse
	protected String command;

	// our gcode parser
	protected GCodeParser parser;
	
	//our tool drivers
	protected ToolDriver[] tools;
	protected ToolDriver currentTool;

	//our current
	protected Point3d current;

	/**
	  * Creates the driver object.
	  */
	public Driver()
	{
		parser = new GCodeParser();
		
		current = new Point3d();
	}

	public void parse(String cmd)
	{
		command = cmd;
		driver.parse(cmd);
	}

	public void handleStops() throws JobRewindException, JobEndException, JobCancelledException
	{
		parser.handleStops();
	}
	
	public void execute()
	{
		parser.execute(this);
	}
	
	protected boolean showContinueDialog(String message)
	{
		int result = JOptionPane.showConfirmDialog(null, message, "Continue Build?", JOptionPane.YES_NO_OPTION);
		
		if (result == JOptionPane.YES_OPTION)
			return true;
		else
			return false;
	}
	
	/**
	 * Create and instantiate the driver class for our particular machine
	 * @param String name the name of the driver to instantiate
	 * @return Driver a driver object ready for parsing / running gcode
	 */
	public static Driver factory(Node xml)
	{
		NodeList kids = xml.getChildNodes();

		for (int j=0; j<kids.getLength(); j++)
		{
			Node kid = kids.item(j);

			if (kid.getNodeName().equals("name"))
			{
				String driverName = kid.getFirstChild().getNodeValue().trim();

				System.out.println("Loading driver: " + driverName);

				//create our driver and give it the config so it can self-configure.
				if (driverName.equals("serialpassthrough"))
					return new SerialPassthroughDriver(xml);
				else
					return new NullDriver();
			}
		}
		
		System.out.println("Failing over to null driver.");
		
		//bail with a simulation driver.
		return new NullDriver();
	}
	
	/**
	* empty parameters?  give up a null driver.
	*/
	public static Driver factory()
	{
		return new NullDriver();
	}
	
	public ToolDriver currentTool()
	{
		return currentTool;
	}
	
	public void setCurrentPosition(Point3d p)
	{
		current = p;
	}
	
	public Point3d getCurrentPosition()
	{
		return current;
	}
	
	public void queuePoint(Point3d p)
	{
	}
	
	public void selectTool(int toolIndex)
	{
		currentTool = tools[toolIndex];
	}
	
	/**
	* sets the feedrate in mm/minute
	*/
	public void setFeedrate(double feed)
	{
	}
	
	public void homeXYZ()
	{
	}
	
	public void homeXY()
	{
	}
	
	public void homeX()
	{
	}
		
	public void homeY()
	{
	}
	
	public void homeZ()
	{
	}
	
	public void delay(long millis)
	{
	}
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
