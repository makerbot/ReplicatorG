/*
  Part of the ReplicatorG project - http://www.replicat.org

  This class takes a gcode command, parses it and then does something with it.

  @author	Hoeken
  @www http://www.zachhoeken.org

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

import java.util.regex.*;

class Driver extends Thread
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
	
	// a message and/or prompt
	protected String message = "";
	
	//our gcode variables
	protected double dCode = -1;
	protected double fCode = -1;
	protected double gGode = -1;
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
	
	/**
	  * Creates the driver object.
	  */
	public Driver()
	{
	}
	
	public boolean parse(String cmd)
	{
		command = cmd;
		
		return true;
	}
	
	public boolean hasCode(char code)
	{
		if (command.indexOf(code) >= 0)
			return true;
		else
			return false;
	}
	
	public double parseCode(char code)
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
	
	public void commandFinished()
	{
		//move us to our target.
		currentX = targetX;
		currentY = targetY;
		currentZ = targetZ;

		//clear our gcodes.
		dCode = -1;
		fCode = -1;
		gGode = -1;
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
	}
	
	public static Driver factory(String name)
	{
		//TODO: add driver subclasses.
		return new Driver();
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
