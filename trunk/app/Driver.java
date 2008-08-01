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
import javax.swing.JOptionPane;

import org.w3c.dom.*;

//import org.xml.sax.*;
//import org.xml.sax.helpers.XMLReaderFactory;

public class Driver
{
	// command to parse
	protected String command;
	
	// machine state varibles
	protected double currentX = 0.0;
	protected double currentY = 0.0;
	protected double currentZ = 0.0;

	// target variables
	protected double targetX = 0.0;
	protected double targetY = 0.0;
	protected double targetZ = 0.0;
	
	// current selected tool
	protected int tool = 0;
	
	// a comment passed in
	protected String comment = "";
	
	//our gcode variables
	protected double dCode = -1;
	protected double fCode = -1;
	protected double gCode = -1;
	protected double hCode = -1;
	protected double iCode = -1;
	protected double jCode = -1;
	protected double kCode = -1;
	protected double lCode = -1;
	protected double mCode = -1;
	protected double nCode = -1;
	protected double pCode = -1;
	protected double qCode = -1;
	protected double rCode = -1;
	protected double sCode = -1;
	protected double tCode = -1;
	protected double xCode = -1;
	protected double yCode = -1;
	protected double zCode = -1;
	
	Pattern parenPattern;
	Pattern semiPattern;
	
	/**
	  * Creates the driver object.
	  */
	public Driver()
	{
		parenPattern = Pattern.compile("\\((.*)\\)");
		semiPattern = Pattern.compile(";(.*)");
	}
	
	/**
	 * Parses a line of GCode, sets up the variables, etc.
	 * @param String cmd a line of GCode to parse
	 */
	public boolean parse(String cmd)
	{
		//save our command
		command = cmd;

		parseComments();
		stripComments();
		
		//parse all our codes
		dCode = parseCode('D');
		fCode = parseCode('F');
		gCode = parseCode('G');
		hCode = parseCode('H');
		iCode = parseCode('I');
		jCode = parseCode('J');
		kCode = parseCode('K');
		lCode = parseCode('L');
		mCode = parseCode('M');
		pCode = parseCode('P');
		qCode = parseCode('Q');
		rCode = parseCode('R');
		sCode = parseCode('S');
		tCode = parseCode('T');
		xCode = parseCode('X');
		yCode = parseCode('Y');
		zCode = parseCode('Z');
		
		
		return true;
	}

	public void handleStops() throws JobRewindException, JobEndException, JobCancelledException
	{
		String message = "";
		int result = 0;
		
		if (mCode == 0)
		{
			if (comment.length() > 0)
				message = "Automatic Halt: " + comment;
			else
				message = "Automatic Halt";
				
			if (!showContinueDialog(message))
				throw new JobCancelledException();
		}
		else if (mCode == 1 && Preferences.getBoolean("machine.optionalstops"))
		{
			if (comment.length() > 0)
				message = "Optional Halt: " + comment;
			else
				message = "Optional Halt";

			if (!showContinueDialog(message))
				throw new JobCancelledException();
		}
		else if (mCode == 2)
		{
			if (comment.length() > 0)
				message = "Program End: " + comment;
			else
				message = "Program End";
		
			JOptionPane.showMessageDialog(null, message);
			
			throw new JobEndException();
		}
		else if (mCode == 30)
		{
			if (comment.length() > 0)
				message = "Program Rewind: " + comment;
			else
				message = "Program Rewind";
		
			if (!showContinueDialog(message))
				throw new JobCancelledException();
			
			commandFinished();
			throw new JobRewindException();
		}
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
	 * Checks to see if our current line of GCode has this particular code
	 * @param char code the code to check for (G, M, X, etc.)
	 * @return boolean if the code was found or not
	 */
	private boolean hasCode(char code)
	{
		if (command.indexOf(code) >= 0)
			return true;
		else
			return false;
	}
	
	private void parseComments()
	{
		Matcher parenMatcher = parenPattern.matcher(command);
		Matcher semiMatcher = semiPattern.matcher(command);

		if (parenMatcher.find())
			comment = parenMatcher.group(1);

		if (semiMatcher.find())
			comment = semiMatcher.group(1);
			
		//clean it up.
		comment = comment.trim();
		comment = comment.replace('|', '\n');

		//echo it?
		//if (comment.length() > 0)
		//	System.out.println(comment);
	}
	
	private void stripComments()
	{
		Matcher parenMatcher = parenPattern.matcher(command);
		command = parenMatcher.replaceAll("");

		Matcher semiMatcher = semiPattern.matcher(command);
		command = semiMatcher.replaceAll("");
	}
	
	/**
	 * Finds out the value of the code we're looking for
	 * @param char code the code whose value we're looking up
	 * @return double the value of the code, -1 if not found.
	 */
	private double parseCode(char code)
	{
		Pattern myPattern = Pattern.compile(code + "([0-9.+-]+)");
		Matcher myMatcher = myPattern.matcher(command);
		
		if (hasCode(code))
		{
			if (myMatcher.find())
			{
				String match = myMatcher.group(1);
				double number = Double.parseDouble(match);

				return number;
			}
		}
		
		//bail/fail with a -1
		return -1;
	}
	
	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void execute()
	{
		
	}
	
	/**
	 * Prepare us for the next gcode command to come in.
	 */
	public void commandFinished()
	{
		//move us to our target.
		currentX = targetX;
		currentY = targetY;
		currentZ = targetZ;

		//clear our gcodes.
		dCode = -1;
		fCode = -1;
		gCode = -1;
		hCode = -1;
		iCode = -1;
		jCode = -1;
		kCode = -1;
		lCode = -1;
		mCode = -1;
		nCode = -1;
		pCode = -1;
		qCode = -1;
		rCode = -1;
		sCode = -1;
		tCode = -1;
		xCode = -1;
		yCode = -1;
		zCode = -1;
		
		comment = "";
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
					return new SimulationDriver(xml);
			}
		}
		
		System.out.println("Failing over to simulation driver.");
		
		//bail with a simulation driver.
		return new SimulationDriver();
	}
	
	/**
	* empty parameters?  give up a simulation driver.
	*/
	public static Driver factory()
	{
		return new SimulationDriver();
	}
	
	// our getter functions
	//TODO: add getPosition(x, y, z) function and convert all to use it
	public double getCurrentX() { return currentX; }
	public double getCurrentY() { return currentY; }
	public double getCurrentZ() { return currentZ; }
	
	//TODO: add getTarget(x, y, z) function and convert all to use it
	public double getTargetX() { return targetX; }
	public double getTargetY() { return targetY; }
	public double getTargetZ() { return targetZ; }
	
	// our setter functions
	//TODO: add setCurrentPosition(x, y, z) and convert all to use it
	public void setCurrentX(double x) { currentX = x; }
	public void setCurrentY(double y) { currentY = y; }
	public void setCurrentZ(double z) { currentZ = z; }
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
