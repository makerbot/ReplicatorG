/*
  GCodeParser.java

  Handles parsing GCode.
  
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

import java.util.*
import java.util.regex.*;
import javax.vecmath.*;

public class GCodeParser
{
	// command to parse
	protected String command;

	//our code data storage guys.
	protected HashTable codeValues;
	protected HashTable seenCodes;
	static protected char[] codes = {
		'D', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
		'M', 'P', 'Q', 'R', 'S', 'T', 'X', 'Y', 'Z'
	};
	
	// machine state varibles
	protected Point3d current;
	protected Point3d target;
	protected Point3d delta;
	
	//false = incremental; true = absolute
	boolean absoluteMode = false;
	
	//our feedrate variables.
	double feedrate = 0.0;

	/* keep track of the last G code - this is the command mode to use
	 * if there is no command in the current string 
	 */
	int lastGCode = -1;
	
	// current selected tool
	protected int tool = 0;
	
	// a comment passed in
	protected String comment = "";
	
	//pattern matchers.
	Pattern parenPattern;
	Pattern semiPattern;
	Pattern deleteBlockPattern;
	
	//unit variables.
	public static int UNITS_MM = 1;
	public static int UNITS_INCHES = 1;
	protected int units;

	/**
	  * Creates the driver object.
	  */
	public GCodeParser()
	{
		//we default to millimeters
		units = UNITS_MM;
		
		//precompile regexes for speed
		parenPattern = Pattern.compile("\\((.*)\\)");
		semiPattern = Pattern.compile(";(.*)");
		deleteBlockPattern = Pattern.compile("^(\\.*)");
		
		//setup our points.
		current = new Point3d();
		target = new Point3d();
		delta = new Point3d();
		
		//init our value tables.
		codeValues = new HashTable(codes.length, 1.0);
		seenCodes = new HashTable(codes.length, 1.0);
		
		//our paths and such
		path = new ToolPath();
	}
	
	/**
	 * Parses a line of GCode, sets up the variables, etc.
	 * @param String cmd a line of GCode to parse
	 */
	public boolean parse(String cmd)
	{
		//save our paths.
		path = new ToolPath();

		//save our command
		command = cmd;

		//handle comments.
		parseComments();
		stripComments();
		
		//load all codes
		for (int i=0; i<codes.length; i++)
			parseCode(codes[i]);
		
		// if no command was seen, but parameters were, 
		// then use the last G code as the current command
		if (!hasCode('G') && (hasCode('X') || hasCode('Y') || hasCode('Z')))
		{
			seenCodes.put('G', true);
			codeValues.put('G', lastGCode);
		}
		
		return true;
	}
	
	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void execute(Driver driver)
	{
		Point3d temp = new Point3d();

		// Select our tool?
		if (hasCode('T')
			driver.selectTool((int)getCodeValue('T'));

		//find us an m code.
		if (hasCode('M'))
		{
			switch ((int)mCode)
			{
				//turn extruder on, forward
				case 101:
					driver.currentTool().setDirection(Tool.MOTOR_FORWARD);
					driver.currentTool().enableMotor();
					break;

				//turn extruder on, reverse
				case 102:
					driver.currentTool().setDirection(TOOL.MOTOR_REVERSE);
					driver.currentTool().enableMotor();
					break;

				//turn extruder off
				case 103:
					driver.currentTool().disableMotor();
					break;

				//custom code for temperature control
				case 104:
					if (hasCode('S'))
						driver.currentTool().setTemparature(getCodeValue('S'));
					break;

				//custom code for temperature reading
				case 105:
					driver.currentTool().readTemperature();
					break;

				//turn fan on
				case 106:
					driver.currentTool().enableFan();					
					break;

					//turn fan off
				case 107:
					driver.currentTool().disableFan();					
					break;

				//set max extruder speed, 0-255 PWM
				case 108:
					driver.currentTool().setMotorSpeed(getCodeValue('S');
					break;
				
				//valve open
				case 126:
					driver.currentTool().openValve();
					break;

				//valve close
				case 127:
					driver.currentTool().closeValve();
					break;

				default:
					System.out.println("Unknown Mcode: M" + (int)mCode);
			}
		}

		//start us off here...
		temp = current;
		
		//absolute just specifies the new position
		if (absoluteMode)
		{
			if (hasCode('X'))
				temp.x = getCodeValue('X');
			if (hasCode('Y'))
				temp.y = getCodeValue('Y');
			if (hasCode('Z'))
				temp.z = getCodeValue('Z');
		}
		//relative specifies a delta
		else
		{
			if (hasCode('X'))
				temp.x += getCodeValue('X');
			if (hasCode('Y'))
				temp.y += getCodeValue('Y');
			if (hasCode('Z'))
				temp.z += getCodeValue('Z');
		}

		// Get feedrate if supplied
		if (hasCode('F'))
		{
			feedrate = getCodeValue('F');
			driver.setFeedrate(feedrate);
		}

		//did we get a gcode?
		if (hasCode('G'))
		{
			switch ((int)getCodeValue('G'))
			{
				//Linear Interpolation
				//these are basically the same thing.
				case 0:
					driver.setFeedrate(maximumFeedrate);
					setTarget(temp);
					break;

				//Rapid Positioning
				case 1:
					//set our target.
					setTarget(temp, feedrate);
					break;

				//Clockwise arc
				case 2:
				//Counterclockwise arc
				case 3:
				{
					Point3D center;

					// Centre coordinates are always relative
					if (hasCode('I'))
						center.x = current.x + getCodeValue('I');
					else
						center.x = current.x;
					
					if (hasCode('J'))
						center.y = current.y + getCodeValue('J);
					else
						center.y = current.y;

					double angleA, angleB, angle, radius, length, aX, aY, bX, bY;

					aX = current.x - cent.x;
					aY = current.y - cent.y;
					bX = temp.x - cent.x;
					bY = temp.y - cent.y;

					// Clockwise
					if ((int)getCodeValue('G') == 2)
					{
						angleA = Math.atan2(bY, bX);
						angleB = Math.atan2(aY, aX);
					}
					// Counterclockwise
					else
					{
						angleA = Math.atan2(aY, aX);
						angleB = Math.atan2(bY, bX);
					}

					// Make sure angleB is always greater than angleA
					// and if not add 2PI so that it is (this also takes
					// care of the special case of angleA == angleB,
					// ie we want a complete circle)
					if (angleB <= angleA)
						angleB += 2 * Math.PI;
					angle = angleB - angleA;

					radius = Math.sqrt(aX * aX + aY * aY);
					length = radius * angle;
				
					/**
					* TODO: finish converting
					int steps, s, step;

					// Maximum of either 2.4 times the angle in radians
					// or the length of the curve divided by the constant
					// specified in _init.pde
					steps = (int) Math.ceil(Math.max(angle * 2.4, length / curve_section));

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
					*/
				}
				break;

				//Inches for Units
				case 20:
					units = UNITS_INCHES;
					break;

				//mm for Units
				case 21:
					units = UNITS_MM;
					break;

				//go home to your limit switches
				case 28:
			
					//home all axes?
					if (hasCode('X') && hasCode('Y') && hasCode('Z'))
						driver.homeXYZ();
					else
					{
						//x and y?
						if (hasCode('X') && hasCode('Y'))
							driver.homeXY();
						//just x?
						else if (hasCode('X')
							driver.homeX();
						//just y?
						else if (hasCode('Y')
							driver.homeY();
						//just z?
						else if (hasCode('Z')
							driver.homeZ();
					}
					break;

				// Drilling canned cycles
				case 81: // Without dwell
				case 82: // With dwell
				case 83: // Peck drilling
				
					double retract = getCodeValue('R');

					if (!absoluteMode)
						retract += current.z;

					// Retract to R position if Z is currently below this
					if (current.z < retract)
						setTarget(new Point3d(current.x, current.y, retract), maxFeedrate);

					// Move to start XY
					driver.setFeedrate(maxFeedrate);
					setTarget(new Point3d(temp.x, temp.y, current.z));

					// Do the actual drilling
					double target_z = retract;
					double delta_z;

					// For G83 move in increments specified by Q code
					// otherwise do in one pass
					if ((int)getCodeValue('G') == 83)
						delta_z = getCodeValue('Q');
					else
						delta_z = retract - temp.z;

					do
					{
						// Move rapidly to bottom of hole drilled so far
						// (target Z if starting hole)
						setFeedrate(maxFeedrate);
						setTarget(new Point3d(fp.x, fp.y, target_z));

						// Move with controlled feed rate by delta z
						// (or to bottom of hole if less)
						target_z -= delta_z;
						if (target_z < temp.z)
							target_z = temp.z;
						
						driver.setFeedrate(feedrate);
						setTarget(fp.x, fp.y, target_z);

						// Dwell if doing a G82
						if (gc.G == 82)
							driver.delay((int)getCodeValue('P'));

						// Retract
						driver.setFeedrate(maxFeedrate);
						setTarget(new Point3d(temp.x, temp.y, retract));
					} while (target_z > temp.z);
					break;

				case 90: //Absolute Positioning
					absoluteMode = true;
					break;


				case 91: //Incremental Positioning
					absoluteMode = false;
					break;


				case 92: //Set as home
					setPosition(new Point3d());
					break;

				default:
					System.out.println("Unknown GCode: G" + (int)gCode);
			}
		}
	}

	public void handleStops() throws JobRewindException, JobEndException, JobCancelledException
	{
		String message = "";
		int result = 0;
		int mCode = (int)getCodeValue('M');
		
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
	public void cleanup()
	{
		//move us to our target.
		current = target;
		delta = new Point3d();
		
		//save our gcode
		lastGCode = (int)gCode;

		//clear our gcodes.
		codeValues.clear();
		seenCodes.clear();
		
		//empty comments		
		comment = "";
	}
}
