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
import javax.vecmath.*;

import org.w3c.dom.*;

//import org.xml.sax.*;
//import org.xml.sax.helpers.XMLReaderFactory;

public class Driver
{
	// command to parse
	protected String command;
	
	// machine state varibles
	protected Point3d current;
	protected Point3d target;
	protected Point3d delta;
	
	//0 = incremental; 1 = absolute
	boolean abs_mode = false;
	
	//our feedrate variables.
	double feedrate = 0.0;

	/* keep track of the last G code - this is the command mode to use
	 * if there is no command in the current string 
	 */
	int lastGCode = -1;
	
	// current selected tool
	protected int tool = 0;
	boolean extruding = false;
	
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
	
	//pattern matchers.
	Pattern parenPattern;
	Pattern semiPattern;
	Pattern deleteBlockPattern;
	
	/**
	  * Creates the driver object.
	  */
	public Driver()
	{
		parenPattern = Pattern.compile("\\((.*)\\)");
		semiPattern = Pattern.compile(";(.*)");
		deleteBlockPattern = Pattern.compile("^(\\.*)");
		
		current = new Point3d(0.0, 0.0, 0.0);
		target = new Point3d(0.0, 0.0, 0.0);
		delta = new Point3d(0.0, 0.0, 0.0);
	}
	
	/**
	 * Parses a line of GCode, sets up the variables, etc.
	 * @param String cmd a line of GCode to parse
	 */
	public boolean parse(String cmd)
	{
		//save our command
		command = cmd;

		//handle comments.
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
		
		/* if no command was seen, but parameters were, then use the last G code as 
		 * the current command
		 */
		if (!hasCode('G') && (hasCode('X') || hasCode('Y') || hasCode('Z')))
			gCode = lastGCode;
		
		return true;
	}
	
	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void execute()
	{
		Point3d temp = new Point3d();
		
		//did we get a gcode?
		if (gCode >= 0)
		{
			temp = current;
			if (abs_mode)
			{
				if (hasCode('X'))
					temp.x = xCode;
				if (hasCode('Y'))
					temp.y = yCode;
				if (hasCode('Z'))
					temp.z = zCode;
			}
			else
			{
				if (hasCode('X'))
					temp.x += xCode;
				if (hasCode('Y'))
					temp.y += yCode;
				if (hasCode('Z'))
					temp.z += zCode;
			}

			// Get feedrate if supplied
			if (hasCode('F'))
				feedrate = fCode;

			//do something!
			switch ((int)gCode)
			{
				//Rapid Positioning
				//Linear Interpolation
				//these are basically the same thing.
				case 0:
				case 1:
					//set our target.
					setTarget(temp);
					break;

				//Clockwise arc
				case 2:
				//Counterclockwise arc
				case 3:
				/*
				//TODO: make this work.
				{
					FloatPoint cent;

					// Centre coordinates are always relative
					if (gc.seen & GCODE_I) cent.x = current_units.x + gc.I;
					else cent.x = current_units.x;
					if (gc.seen & GCODE_J) cent.y = current_units.y + gc.J;

					float angleA, angleB, angle, radius, length, aX, aY, bX, bY;

					aX = (current_units.x - cent.x);
					aY = (current_units.y - cent.y);
					bX = (fp.x - cent.x);
					bY = (fp.y - cent.y);

					// Clockwise
					if (gc.G == 2)
					{
						angleA = atan2(bY, bX);
						angleB = atan2(aY, aX);
					}
					// Counterclockwise
					else
					{
						angleA = atan2(aY, aX);
						angleB = atan2(bY, bX);
					}

					// Make sure angleB is always greater than angleA
					// and if not add 2PI so that it is (this also takes
					// care of the special case of angleA == angleB,
					// ie we want a complete circle)
					if (angleB <= angleA)
						angleB += 2 * M_PI;
					angle = angleB - angleA;

					radius = sqrt(aX * aX + aY * aY);
					length = radius * angle;
					int steps, s, step;

					// Maximum of either 2.4 times the angle in radians or the length of the curve divided by the constant specified in _init.pde
					steps = (int) ceil(max(angle * 2.4, length / curve_section));

					FloatPoint newPoint;
					float arc_start_z = current_units.z;
					for (s = 1; s <= steps; s++)
					{
						step = (gc.G == 3) ? s : steps - s; // Work backwards for CW
						newPoint.x = cent.x + radius * cos(angleA + angle
								* ((float) step / steps));
						newPoint.y = cent.y + radius * sin(angleA + angle
								* ((float) step / steps));
						set_target(newPoint.x, newPoint.y, arc_start_z + (fp.z
								- arc_start_z) * s / steps);

						// Need to calculate rate for each section of curve
						if (feedrate > 0)
							feedrate_micros = calculate_feedrate_delay(feedrate);
						else
							feedrate_micros = getMaxSpeed();

						// Make step
						dda_move(feedrate_micros);
					}
				}
				*/
				break;


				//Dwell
				case 4:
					//TODO: add delay call.
					//delay((int)(gc.P * 1000));
					break;

					//Inches for Units
				case 20:
					//TODO: figure this out
					break;

					//mm for Units
				case 21:
					//TODO: figure this out
					break;

					//go home.
				case 28:
					setTarget(new Point3d());
					break;

				/*
				//TODO: this may be wrong.
					//go home via an intermediate point.
				case 30:
					//set our target.
					set_target(fp.x, fp.y, fp.z);

					//go there.
					dda_move(getMaxSpeed());

					//go home.
					set_target(0.0, 0.0, 0.0);
					dda_move(getMaxSpeed());
					break;
				*/
				/*
				TODO: still need to do this.
				// Drilling canned cycles
				case 81: // Without dwell
				case 82: // With dwell
				case 83: // Peck drilling
				{
					float retract = gc.R;

					if (!abs_mode)
						retract += current_units.z;

					// Retract to R position if Z is currently below this
					if (current_units.z < retract)
					{
						set_target(current_units.x, current_units.y, retract);
						dda_move(getMaxSpeed());
					}

					// Move to start XY
					set_target(fp.x, fp.y, current_units.z);
					dda_move(getMaxSpeed());

					// Do the actual drilling
					float target_z = retract;
					float delta_z;

					// For G83 move in increments specified by Q code, otherwise do in one pass
					if (gc.G == 83)
						delta_z = gc.Q;
					else
						delta_z = retract - fp.z;

					do {
						// Move rapidly to bottom of hole drilled so far (target Z if starting hole)
						set_target(fp.x, fp.y, target_z);
						dda_move(getMaxSpeed());

						// Move with controlled feed rate by delta z (or to bottom of hole if less)
						target_z -= delta_z;
						if (target_z < fp.z)
							target_z = fp.z;
						set_target(fp.x, fp.y, target_z);
						if (feedrate > 0)
							feedrate_micros = calculate_feedrate_delay(feedrate);
						else
							feedrate_micros = getMaxSpeed();
						dda_move(feedrate_micros);

						// Dwell if doing a G82
						if (gc.G == 82)
							delay((int)(gc.P * 1000));

						// Retract
						set_target(fp.x, fp.y, retract);
						dda_move(getMaxSpeed());
					} while (target_z > fp.z);
				}
				break;
				*/

				case 90: //Absolute Positioning
					abs_mode = true;
					break;


				case 91: //Incremental Positioning
					abs_mode = false;
					break;


				case 92: //Set as home
					setPosition(new Point3d(0.0, 0.0, 0.0));
					break;

					/*
					 //Inverse Time Feed Mode
					 case 93:

					 break;  //TODO: add this

					 //Feed per Minute Mode
					 case 94:

					 break;  //TODO: add this
					 */

				default:
					System.out.println("Unknown GCode: G" + (int)gCode);
			}
		}

		//find us an m code.
		if (hasCode('M'))
		{
			switch ((int)mCode)
			{
				case 101:
					extruding = true;
					//extruder_set_direction(1);
					//extruder_set_speed(extruder_speed);
					break;

					//turn extruder on, reverse
				case 102:
					extruding = false;
					//extruder_set_direction(0);
					//extruder_set_speed(extruder_speed);
					break;

					//turn extruder off
				case 103:
					extruding = false;
					//extruder_set_speed(0);
					break;

					//custom code for temperature control
				case 104:
					/*
					//TODO: handle this
					if (hasCode('S'))
					{
						extruder_set_temperature((int)gc.S);

						//warmup if we're too cold.
						while (extruder_get_temperature() < extruder_target_celsius)
						{
							extruder_manage_temperature();
							Serial.print("T:");
							Serial.println(extruder_get_temperature());
							delay(1000);
						}
					}
					*/
					break;

					//custom code for temperature reading
				case 105:
					/*
					TODO: handle this
					Serial.print("T:");
					Serial.println(extruder_get_temperature());
					*/
					break;

					//turn fan on
				case 106:
					//extruder_set_cooler(255);
					break;

					//turn fan off
				case 107:
					//extruder_set_cooler(0);
					break;

				//set max extruder speed, 0-255 PWM
				case 108:
					/*
					if (gc.seen & GCODE_S)
						extruder_speed = (int)gc.S;
					*/
					break;
					
				//valve open
				case 126:
					break;

				//valve close
				case 127:
					break;
					

				default:
					System.out.println("Unknown Mcode: M" + (int)mCode);
			}
		}
	}

	protected void setPosition(Point3d p)
	{
		current = p;
		
		calculateDeltas();
	}

	protected void setTarget(Point3d t)
	{
		target = t;

		//System.out.println("target: " + t.x + ", " + t.y + ", " + t.z);
		
		calculateDeltas();
	}
	
	protected void calculateDeltas()
	{
		//figure our deltas.
		delta.x = Math.abs(target.x - current.x);
		delta.y = Math.abs(target.y - current.y);
		delta.z = Math.abs(target.z - current.z);
		
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
			//send a 0 to that its noted somewhere.
			else
				return 0;
		}
		
		//bail/fail with a -1
		return -1;
	}
	
	/**
	 * Prepare us for the next gcode command to come in.
	 */
	public void commandFinished()
	{
		//move us to our target.
		current = target;
		delta = new Point3d(0.0, 0.0, 0.0);
		
		//save our gcode
		lastGCode = (int)gCode;

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
	public double getCurrentX() { return current.x; }
	public double getCurrentY() { return current.y; }
	public double getCurrentZ() { return current.z; }
	
	//TODO: add getTarget(x, y, z) function and convert all to use it
	public double getTargetX() { return target.x; }
	public double getTargetY() { return target.y; }
	public double getTargetZ() { return target.z; }
	
	// our setter functions
	//TODO: add setCurrentPosition(x, y, z) and convert all to use it
	public void setCurrentX(double x) { current.x = x; }
	public void setCurrentY(double y) { current.y = y; }
	public void setCurrentZ(double z) { current.z = z; }
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
