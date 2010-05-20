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

package replicatorg.app;

import java.lang.reflect.Method;

import java.util.EnumSet;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.Point3d;

import replicatorg.app.exceptions.GCodeException;
import replicatorg.app.exceptions.JobCancelledException;
import replicatorg.app.exceptions.JobEndException;
import replicatorg.app.exceptions.JobException;
import replicatorg.app.exceptions.JobRewindException;
import replicatorg.drivers.Driver;
import replicatorg.machine.model.Axis;
import replicatorg.machine.model.ToolModel;

public class GCodeParser {
	// command to parse
	protected String command;

	// our driver we use.
	protected Driver driver;

	// our code data storage guys.
	protected Hashtable<String, Double> codeValues;

	protected Hashtable<String, Boolean> seenCodes;

	static protected String[] codes = { "D", "F", "G", "H", "I", "J", "K", "L",
			"M", "P", "Q", "R", "S", "T", "X", "Y", "Z" };

	// our curve section variables.
	public static double curveSectionMM = Base.preferences.getDouble("replicatorg.parser.curve_segment_mm", 1.0);
	public static double curveSectionInches = curveSectionMM / 25.4;

	protected double curveSection = 0.0;

	// our plane selection variables
	protected static int XY_PLANE = 0;

	protected static int ZX_PLANE = 1;

	protected static int ZY_PLANE = 2;

	protected int currentPlane = 0;

	// our offset variables 0 = master, 1-6 = offsets 1-6
	protected Point3d currentOffset;

	// machine state varibles
	//protected Point3d current;

	protected Point3d target;

	protected Point3d delta;

	// false = incremental; true = absolute
	boolean absoluteMode = false;

	// our feedrate variables.
	double feedrate = 0.0;

	/*
	 * keep track of the last G code - this is the command mode to use if there
	 * is no command in the current string
	 */
	int lastGCode = -1;

	// current selected tool
	protected int tool = -1;

	// a comment passed in
	protected String comment = "";

	// pattern matchers.
	Pattern parenPattern;

	Pattern semiPattern;

	Pattern deleteBlockPattern;

	// unit variables.
	public static int UNITS_MM = 0;

	public static int UNITS_INCHES = 1;

	protected int units;

	// canned drilling cycle variables
	protected Point3d drillTarget;

	protected double drillRetract = 0.0;

	protected double drillFeedrate = 0.0;

	protected int drillDwell = 0;

	protected double drillPecksize = 0.0;

	// TwitterBot extension variables
        protected Class extClass;
        protected Object objExtClass;
	public static final String TB_CODE = "M";
	public static final int TB_INIT = 997;
	public static final int TB_MESSAGE = 998;
	public static final int TB_CLEANUP = 999;
	
	/**
	 * Creates the driver object.
	 */
	public GCodeParser() {
		// we default to millimeters
		units = UNITS_MM;
		curveSection = curveSectionMM;

		// precompile regexes for speed
		parenPattern = Pattern.compile("\\((.*)\\)");
		semiPattern = Pattern.compile(";(.*)");
		deleteBlockPattern = Pattern.compile("^(\\.*)");

		// setup our points.
//		current = new Point3d();
//		System.err.println("-CURRENT "+current.toString());
		target = new Point3d();
		delta = new Point3d();
		drillTarget = new Point3d();

		// init our value tables.
		codeValues = new Hashtable<String, Double>(codes.length, 1);
		seenCodes = new Hashtable<String, Boolean>(codes.length, 1);

		// init our offset
		currentOffset = new Point3d();
	}

	/**
	 * Get the maximum feed rate from the driver's model.
	 */
	protected double getMaxFeedrate() {
		// TODO: right now this is defaulting to the x feedrate. We should
		// eventually check for z-axis motions and use that feedrate. We should
		// also either alter the model, or post a warning when the x and y
		// feedrates differ.
		return driver.getMachine().getMaximumFeedrates().x;
	}

	/**
	 * initialize parser with values from the driver
	 */
	public void init(Driver drv) {
		// our driver class
		driver = drv;

		// init our offset variables
		currentOffset = driver.getOffset(0);
	}

	/**
	 * Parses a line of GCode, sets up the variables, etc.
	 * 
	 * @param String
	 *            cmd a line of GCode to parse
	 */
	public boolean parse(String cmd) {
		// get ready for last one.
		cleanup();

		// save our command
		command = cmd;

		// handle comments.
		parseComments();
		stripComments();

		// load all codes
		for (int i = 0; i < codes.length; i++) {
			double value = parseCode(codes[i]);
			codeValues.put(new String(codes[i]), new Double(value));
		}

		// if no command was seen, but parameters were,
		// then use the last G code as the current command
		if (!hasCode("G") && (hasCode("X") || hasCode("Y") || hasCode("Z"))) {
			seenCodes.put(new String("G"), new Boolean(true));
			codeValues.put(new String("G"), new Double(lastGCode));
		}

		return true;
	}

	private boolean findCode(String code) {
		if (command.indexOf(code) >= 0)
			return true;
		else
			return false;
	}

	public double convertToMM(double value, int units) {
		if (units == UNITS_INCHES) {
			return value * 25.4;
		}
		return value;
	}

	public double getCodeValue(String c) {
		Double d = (Double) codeValues.get(c);

		if (d != null)
			return d.doubleValue();
		else
			return 0.0;
	}

	/**
	 * Checks to see if our current line of GCode has this particular code
	 * 
	 * @param char
	 *            code the code to check for (G, M, X, etc.)
	 * @return boolean if the code was found or not
	 */
	private boolean hasCode(String code) {
		Boolean b = (Boolean) seenCodes.get(code);

		if (b != null)
			return b.booleanValue();
		else
			return false;
	}

	/**
	 * Finds out the value of the code we're looking for
	 * 
	 * @param char
	 *            code the code whose value we're looking up
	 * @return double the value of the code, -1 if not found.
	 */
	private double parseCode(String code) {
		Pattern myPattern = Pattern.compile(code + "([0-9.+-]+)");
		Matcher myMatcher = myPattern.matcher(command);

		if (findCode(code)) {
			seenCodes.put(code, new Boolean(true));

			if (myMatcher.find()) {
				String match = myMatcher.group(1);
				double number = Double.parseDouble(match);

				return number;
			}
			// send a 0 to that its noted somewhere.
			else
				return 0.0;
		}

		// bail with something relatively harmless
		return 0.0;
	}

	private void parseComments() {
		Matcher parenMatcher = parenPattern.matcher(command);
		Matcher semiMatcher = semiPattern.matcher(command);

		if (parenMatcher.find())
			comment = parenMatcher.group(1);

		if (semiMatcher.find())
			comment = semiMatcher.group(1);

		// clean it up.
		comment = comment.trim();
		comment = comment.replace('|', '\n');

		// echo it?
		// if (comment.length() > 0)
		// System.out.println(comment);
	}

	private void stripComments() {
		Matcher parenMatcher = parenPattern.matcher(command);
		command = parenMatcher.replaceAll("");

		Matcher semiMatcher = semiPattern.matcher(command);
		command = semiMatcher.replaceAll("");
	}

	public String getCommand() {
		return new String(command);
	}

	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void execute() throws GCodeException {
		// TODO: is this the proper way?
		// Set spindle speed?
		// if (hasCode("S"))
		// driver.setSpindleRPM(getCodeValue("S"));

		// execute our other codes
		executeMCodes();
		executeGCodes();

		// Select our tool?
		int tempTool = (int) getCodeValue("T");
		if (hasCode("T")) {
			if (tempTool != tool)
				driver.selectTool(tempTool);

			tool = tempTool;
		}
	}

	private void executeMCodes() throws GCodeException {
		// find us an m code.
		if (hasCode("M")) {
			switch ((int) getCodeValue("M")) {
			// stop codes... handled by handleStops();
			case 0:
			case 1:
			case 2:
				break;

			// spindle on, CW
			case 3:
				driver.setSpindleDirection(ToolModel.MOTOR_CLOCKWISE);
				driver.enableSpindle();
				break;

			// spindle on, CCW
			case 4:
				driver.setSpindleDirection(ToolModel.MOTOR_COUNTER_CLOCKWISE);
				driver.enableSpindle();
				break;

			// spindle off
			case 5:
				driver.disableSpindle();
				break;

			// tool change
			case 6:
				if (hasCode("T"))
					driver.requestToolChange((int) getCodeValue("T"));
				else
					throw new GCodeException(
							"The T parameter is required for tool changes. (M6)");
				break;

			// coolant A on (flood coolant)
			case 7:
				driver.enableFloodCoolant();
				break;

			// coolant B on (mist coolant)
			case 8:
				driver.enableMistCoolant();
				break;

			// all coolants off
			case 9:
				driver.disableFloodCoolant();
				driver.disableMistCoolant();
				break;

			// close clamp
			case 10:
				if (hasCode("Q"))
					driver.closeClamp((int) getCodeValue("Q"));
				else
					throw new GCodeException(
							"The Q parameter is required for clamp operations. (M10)");
				break;

			// open clamp
			case 11:
				if (hasCode("Q"))
					driver.openClamp((int) getCodeValue("Q"));
				else
					throw new GCodeException(
							"The Q parameter is required for clamp operations. (M11)");
				break;

			// spindle CW and coolant A on
			case 13:
				driver.setSpindleDirection(ToolModel.MOTOR_CLOCKWISE);
				driver.enableSpindle();
				driver.enableFloodCoolant();
				break;

			// spindle CW and coolant A on
			case 14:
				driver.setSpindleDirection(ToolModel.MOTOR_COUNTER_CLOCKWISE);
				driver.enableSpindle();
				driver.enableFloodCoolant();
				break;

			// enable drives
			case 17:
				driver.enableDrives();
				break;

			// disable drives
			case 18:
				driver.disableDrives();
				break;

			// open collet
			case 21:
				driver.openCollet();

				// open collet
			case 22:
				driver.closeCollet();

				// M40-M46 = change gear ratios
			case 40:
				driver.changeGearRatio(0);
				break;
			case 41:
				driver.changeGearRatio(1);
				break;
			case 42:
				driver.changeGearRatio(2);
				break;
			case 43:
				driver.changeGearRatio(3);
				break;
			case 44:
				driver.changeGearRatio(4);
				break;
			case 45:
				driver.changeGearRatio(5);
				break;
			case 46:
				driver.changeGearRatio(6);
				break;

			// M48, M49: i dont understand them yet.

			// read spindle speed
			case 50:
				driver.getSpindleRPM();

				// subroutine functions... will implement later
				// case 97: jump
				// case 98: jump to subroutine
				// case 99: return from sub

				// turn extruder on, forward
			case 101:
				driver.setMotorDirection(ToolModel.MOTOR_CLOCKWISE);
				driver.enableMotor();
				break;

			// turn extruder on, reverse
			case 102:
				driver.setMotorDirection(ToolModel.MOTOR_COUNTER_CLOCKWISE);
				driver.enableMotor();
				break;

			// turn extruder off
			case 103:
				driver.disableMotor();
				break;

			// custom code for temperature control
			case 104:
				if (hasCode("S"))
					driver.setTemperature(getCodeValue("S"));
				break;

			// custom code for temperature reading
			case 105:
				driver.readTemperature();
				break;

			// turn fan on
			case 106:
				driver.enableFan();
				break;

			// turn fan off
			case 107:
				driver.disableFan();
				break;

			// set max extruder speed, RPM
			case 108:
				if (hasCode("S"))
					driver
							.setMotorSpeedPWM((int) Math
									.round(getCodeValue("S")));
				else if (hasCode("R"))
					driver.setMotorRPM(getCodeValue("R"));
				break;

			// set build platform temperature
			case 109:
				if (hasCode("S"))
					driver.setPlatformTemperature(getCodeValue("S"));
				break;
				//driver.setPlatformTemperature(getCodeValue("S"));

			// set build chamber temperature
			case 110:
				driver.setChamberTemperature(getCodeValue("S"));
				
			// valve open
			case 126:
				driver.openValve();
				break;

			// valve close
			case 127:
				driver.closeValve();
				break;

			// where are we?
			case 128:
				driver.getPosition();
				break;

			// how far can we go?
			case 129:
				// driver.getRange();
				break;

			// you must know your limits
			case 130:
				// driver.setRange();
				break;

			// initialize to default state.
			case 200:
				driver.initialize();
				break;

			// buffer info
			case 201:
				// driver.getBufferSize();
				break;

			// buffer management
			case 202:
				// driver.clearBuffer();
				break;

			// for killing jobs
			case 203:
				// driver.abort();
				break;

			// temporarily stop printing.
			case 204:
				// driver.pause();
				break;

           case TB_INIT:
               // initialize extension
			// Syntax: M997 ClassName param,param,param,etc
			// your class needs to know how to process the params as it will just
			//receive a string.
			//This code uses a space delimiter
			//To do: should be more general purpose

               try {
                   String params[] = command.split(" ");                       

                   extClass = Class.forName(params[1]); //class name
                   objExtClass = extClass.newInstance();
                   String methParam = params[2];  //initialization params
                   Method extMethod = extClass.getMethod("initialize", new Class[] {String.class});
                   extMethod.invoke(objExtClass, new Object[] {methParam});
               } catch (Exception e) {
                   e.printStackTrace();
               }
               break;

            case TB_MESSAGE:
                // call extension
				// Syntax: M998 Method_name 'Content goes here'
				// passed content is single quote delimited
				// To do: clean up, should be more flexible

                try {
                    String params[] = command.split(" "); //method is param[1]
                    String params2[] = command.split("\\'"); //params to pass are params2[1]

                    String methParam = params2[1]; //params to pass
                    Method extMethod = extClass.getMethod(params[1], new Class[] {String.class});  //method to call
                    extMethod.invoke(objExtClass, new Object[] {methParam});
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case TB_CLEANUP:
                // cleanup extension
				// are explicit nulls needed?
                extClass = null;
                objExtClass = null;
                break;
			default:
				throw new GCodeException("Unknown M code: M"
						+ (int) getCodeValue("M"));
			}
		}
	}

	private void executeGCodes() throws GCodeException {
		// start us off at our current position...
		Point3d temp = driver.getCurrentPosition();

		// initialize our points, etc.
		double iVal = convertToMM(getCodeValue("I"), units); // / X offset
																// for arcs
		double jVal = convertToMM(getCodeValue("J"), units); // / Y offset
																// for arcs
		@SuppressWarnings("unused")
		double kVal = convertToMM(getCodeValue("K"), units); // / Z offset
																// for arcs
		@SuppressWarnings("unused")
		double qVal = convertToMM(getCodeValue("Q"), units); // / feed
																// increment for
																// G83
		double rVal = convertToMM(getCodeValue("R"), units); // / arc radius
		double xVal = convertToMM(getCodeValue("X"), units); // / X units
		double yVal = convertToMM(getCodeValue("Y"), units); // / Y units
		double zVal = convertToMM(getCodeValue("Z"), units); // / Z units

		// adjust for our offsets
		xVal += currentOffset.x;
		yVal += currentOffset.y;
		zVal += currentOffset.z;

		// absolute just specifies the new position
		if (absoluteMode) {
			if (hasCode("X"))
				temp.x = xVal;
			if (hasCode("Y"))
				temp.y = yVal;
			if (hasCode("Z"))
				temp.z = zVal;
		}
		// relative specifies a delta
		else {
			if (hasCode("X"))
				temp.x += xVal;
			if (hasCode("Y"))
				temp.y += yVal;
			if (hasCode("Z"))
				temp.z += zVal;
		}

		// Get feedrate if supplied
		if (hasCode("F")) {
			feedrate = getCodeValue("F");
			driver.setFeedrate(feedrate);
		}

		// did we get a gcode?
		if (hasCode("G")) {
			int gCode = (int) getCodeValue("G");

			switch (gCode) {
			// Linear Interpolation
			// these are basically the same thing.
			case 0:
				driver.setFeedrate(getMaxFeedrate());
				setTarget(temp);
				break;

			// Rapid Positioning
			case 1:
				// set our target.
				driver.setFeedrate(feedrate);
				setTarget(temp);
				break;

			// Clockwise arc
			case 2:
				// Counterclockwise arc
			case 3: {
				// call our arc drawing function.
				if (hasCode("I") || hasCode("J")) {
					// our centerpoint
					Point3d center = new Point3d();
					Point3d current = driver.getCurrentPosition();
					center.x = current.x + iVal;
					center.y = current.y + jVal;

					// draw the arc itself.
					if (gCode == 2)
						drawArc(center, temp, true);
					else
						drawArc(center, temp, false);
				}
				// or we want a radius based one
				else if (hasCode("R")) {
					Base.logger.warning("G02/G03 arcs with (R)adius parameter are not supported yet.");
					if (gCode == 2)
						drawRadius(temp, rVal, true);
					else
						drawRadius(temp, rVal, false);
				}
			}
				break;

			// dwell
			case 4:
				driver.delay((long) getCodeValue("P"));
				break;

			// plane selection codes
			case 17:
				currentPlane = XY_PLANE;
				break;
			case 18:
				Base.logger.warning("ZX Plane moves are not supported yet.");
				currentPlane = ZX_PLANE;
				break;
			case 19:
				Base.logger.warning("ZY Plane moves are not supported yet.");
				currentPlane = ZY_PLANE;
				break;

			// Inches for Units
			case 20:
			case 70:
				units = UNITS_INCHES;
				curveSection = curveSectionInches;
				break;

			// mm for Units
			case 21:
			case 71:
				units = UNITS_MM;
				curveSection = curveSectionMM;
				break;

			// go home to your limit switches
			case 28:
				// home all axes?
				EnumSet<Axis> axes = EnumSet.noneOf(Axis.class);
				
				if (hasCode("X")) axes.add(Axis.X);
				if (hasCode("Y")) axes.add(Axis.Y);
				if (hasCode("Z")) axes.add(Axis.Z);
				driver.homeAxes(axes, false);
				break;

			// single probe
			case 31:
				Base.logger.warning("Single point probes not yet supported.");

				// set our target.
				setTarget(temp);
				// eventually add code to support reading value
				break;

			// probe area
			case 32:

				Base.logger.warning("Area probes not yet supported.");
				{
					Point3d current = driver.getCurrentPosition();
					double minX = current.x;
					double minY = current.y;
					double maxX = xVal;
					double maxY = yVal;
					double increment = iVal;
	
					probeArea(minX, minY, maxX, maxY, increment);
				}
				break;

			// master offset
			case 53:
				currentOffset = driver.getOffset(0);
				break;
			// fixture offset 1
			case 54:
				currentOffset = driver.getOffset(1);
				break;
			// fixture offset 2
			case 55:
				currentOffset = driver.getOffset(2);
				break;
			// fixture offset 3
			case 56:
				currentOffset = driver.getOffset(3);
				break;
			// fixture offset 4
			case 57:
				currentOffset = driver.getOffset(4);
				break;
			// fixture offset 5
			case 58:
				currentOffset = driver.getOffset(5);
				break;
			// fixture offset 6
			case 59:
				currentOffset = driver.getOffset(6);
				break;

			// Peck Motion Cycle
			// case 178: //speed peck motion
			// case 78:
			// TODO: make this

			// Cancel drill cycle
			case 80:
				drillTarget = new Point3d();
				drillRetract = 0.0;
				drillFeedrate = 0.0;
				drillDwell = 0;
				drillPecksize = 0.0;
				break;

			// Drilling canned cycles
			case 81: // Without dwell
			case 82: // With dwell
			case 83: // Peck drilling (w/ optional dwell)
			case 183: // Speed peck drilling (w/ optional dwell)

				// we dont want no stinkin speedpeck
				boolean speedPeck = false;

				// setup our parameters
				if (hasCode("X"))
					drillTarget.x = temp.x;
				if (hasCode("Y"))
					drillTarget.y = temp.y;
				if (hasCode("Z"))
					drillTarget.z = temp.z;
				if (hasCode("F"))
					drillFeedrate = getCodeValue("F");
				if (hasCode("R"))
					drillRetract = rVal;

				// set our vars for normal drilling
				if (gCode == 81) {
					drillDwell = 0;
					drillPecksize = 0.0;
				}
				// they want a dwell
				else if (gCode == 82) {
					if (hasCode("P"))
						drillDwell = (int) getCodeValue("P");
					drillPecksize = 0.0;
				}
				// fancy schmancy 'pecking' motion.
				else if (gCode == 83 || gCode == 183) {
					if (hasCode("P"))
						drillDwell = (int) getCodeValue("P");
					if (hasCode("Q"))
						drillPecksize = Math.abs(getCodeValue("Q"));

					// oooh... do it fast!
					if (gCode == 183)
						speedPeck = true;
				}

				drillingCycle(speedPeck);
				break;

			// Absolute Positioning
			case 90:
				absoluteMode = true;
				break;

			// Incremental Positioning
			case 91:
				absoluteMode = false;
				break;

			// Set position
			case 92:

				Point3d current = driver.getCurrentPosition();

				if (hasCode("X"))
					current.x = xVal;
				if (hasCode("Y"))
					current.y = yVal;
				if (hasCode("Z"))
					current.z = zVal;

				driver.setCurrentPosition(current);
//				this.current = current;
//				System.err.println("-CURRENT "+current.toString());
				this.target = current;
				break;

			// feed rate mode
			// case 93: //inverse time feed rate
			case 94: // IPM feed rate (our default)
				// case 95: //IPR feed rate
				// TODO: make this work.
				break;

			// spindle speed rate
			case 97:
				driver.setSpindleRPM((int) getCodeValue("S"));
				break;

			// error, error!
			default:
				throw new GCodeException("Unknown G code: G"
						+ (int) getCodeValue("G"));
			}
		}
	}

	/*
	 * drillTarget = new Point3d(); drillRetract = 0.0; drillFeedrate = 0.0;
	 * drillDwell = 0.0; drillPecksize = 0.0;
	 */
	private void drillingCycle(boolean speedPeck) {
		// Retract to R position if Z is currently below this
		Point3d current = driver.getCurrentPosition();
		if (current.z < drillRetract) {
			driver.setFeedrate(getMaxFeedrate());
			setTarget(new Point3d(current.x, current.y, drillRetract));
		}

		// Move to start XY
		driver.setFeedrate(getMaxFeedrate());
		setTarget(new Point3d(drillTarget.x, drillTarget.y, current.z));

		// Do the actual drilling
		double targetZ = drillRetract;
		double deltaZ;

		// For G83/G183 move in increments specified by Q code
		if (drillPecksize > 0)
			deltaZ = drillPecksize;
		// otherwise do in one pass
		else
			deltaZ = drillRetract - drillTarget.z;

		do // the drilling
		{
			// only move there if we're not at top
			if (targetZ != drillRetract && !speedPeck) {
				// TODO: move this to 10% of the bottom.
				driver.setFeedrate(getMaxFeedrate());
				setTarget(new Point3d(drillTarget.x, drillTarget.y, targetZ));
			}

			// set our plunge depth
			targetZ -= deltaZ;
			// make sure we dont go too deep.
			if (targetZ < drillTarget.z)
				targetZ = drillTarget.z;

			// Move with controlled feed rate
			driver.setFeedrate(drillFeedrate);

			// do it!
			setTarget(new Point3d(drillTarget.x, drillTarget.y, targetZ));

			// Dwell if doing a G82
			if (drillDwell > 0)
				driver.delay(drillDwell);

			// Retract unless we're speed pecking.
			if (!speedPeck) {
				driver.setFeedrate(getMaxFeedrate());
				setTarget(new Point3d(drillTarget.x, drillTarget.y,
						drillRetract));
			}

		} while (targetZ > drillTarget.z);

		// double check for final speedpeck retract
		if (current.z < drillRetract) {
			driver.setFeedrate(getMaxFeedrate());
			setTarget(new Point3d(drillTarget.x, drillTarget.y, drillRetract));
		}
	}

	private void drawArc(Point3d center, Point3d endpoint, boolean clockwise) {
		// System.out.println("Arc from " + current.toString() + " to " +
		// endpoint.toString() + " with center " + center);

		// angle variables.
		double angleA;
		double angleB;
		double angle;
		double radius;
		double length;

		// delta variables.
		double aX;
		double aY;
		double bX;
		double bY;

		// figure out our deltas
		Point3d current = driver.getCurrentPosition();
		aX = current.x - center.x;
		aY = current.y - center.y;
		bX = endpoint.x - center.x;
		bY = endpoint.y - center.y;

		// Clockwise
		if (clockwise) {
			angleA = Math.atan2(bY, bX);
			angleB = Math.atan2(aY, aX);
		}
		// Counterclockwise
		else {
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
		// calculate a couple useful things.
		radius = Math.sqrt(aX * aX + aY * aY);
		length = radius * angle;

		// for doing the actual move.
		int steps;
		int s;
		int step;

		// Maximum of either 2.4 times the angle in radians
		// or the length of the curve divided by the curve section constant
		steps = (int) Math.ceil(Math.max(angle * 2.4, length / curveSection));

		// this is the real draw action.
		Point3d newPoint = new Point3d();
		double arcStartZ = current.z;
		for (s = 1; s <= steps; s++) {
			// Forwards for CCW, backwards for CW
			if (!clockwise)
				step = s;
			else
				step = steps - s;

			// calculate our waypoint.
			newPoint.x = center.x + radius
					* Math.cos(angleA + angle * ((double) step / steps));
			newPoint.y = center.y + radius
					* Math.sin(angleA + angle * ((double) step / steps));
			newPoint.z = arcStartZ + (endpoint.z - arcStartZ) * s / steps;

			// start the move
			setTarget(newPoint);
		}
	}

	private void drawRadius(Point3d endpoint, double r, boolean clockwise) {
		// not supported!
	}

	private void setTarget(Point3d p) {
		// TODO: wow.
		// This is deeply broken, but I'll leave it be for now and fix with a
		// flag later.
		// If you really want two seperate moves, do it when you generate your
		// toolpath.
		// move z first
		Point3d current = driver.getCurrentPosition();		
		if (p.z != current.z) {
			driver.queuePoint(new Point3d(current.x, current.y, p.z));
		}
		driver.queuePoint(new Point3d(p));
		current = new Point3d(p);
	}

	/**
	 * StopInfo defines an optional or mandatory stop, the message to display with
	 * said stop, and the exceptions to be triggered on success of failure of the
	 * optional stop.
	 * @author phooky
	 *
	 */
	public class StopInfo {
		private final JobException exception;
		private final JobException cancelException;
		private final String message;
		private final boolean optional;
		/**
		 * Create a mandatory stop.
		 * @param message the message to display to the user
		 * @param exception the exception to throw
		 */
		public StopInfo(String message, JobException exception) {
			this.message = message;
			this.optional = false;
			this.exception = this.cancelException = exception;
		}
		/**
		 * Create an optional stop.
		 * @param message the message to display to the user
		 * @param exception the exception to throw if the user confirms the stop
		 * @param cancelException the exception to throw if the user cancels the stop
		 */
		public StopInfo(String message, JobException exception, JobException cancelException) {
			this.message = message;
			this.optional = true;
			this.exception = exception;
			this.cancelException = cancelException;
		}
		public String getMessage() { return message; }
		public JobException getException() { return exception; }
		public JobException getCancelException() { return cancelException; }
		public boolean isOptional() { return optional; }
	}
	
	/**
	 * Return a StopInfo object describing the stop defined by the current code, or null if the
	 * code is not a stop code. 
	 * @return stop information
	 */
	public StopInfo getStops()  {
		String message = null;
		if (comment.length() > 0)
			message = comment;
		int mCode;

		if (hasCode("M")) {
			// we wanna do this after its finished whatever was before.
			driver.waitUntilBufferEmpty();

			mCode = (int) getCodeValue("M");

			if (mCode == 0) {
				// M0 == unconditional halt
				return new StopInfo(message,new JobCancelledException());
			} else if (mCode == 1) {
				// M1 == optional halt
				return new StopInfo(message,null,new JobCancelledException());
			} else if (mCode == 2) {
				// M2 == program end
				return new StopInfo(message,new JobEndException());
			} else if (mCode == 30) {
				if (message.length() == 0) { message = "Program Rewind"; }
				return new StopInfo(message,new JobRewindException(),new JobCancelledException());
			}
		}
		return null;
	}

	/**
	 * probe the Z depth of the point
	 */
	public void probePoint(Point3d point) {

	}

	/**
	 * probe the area specified
	 */
	public void probeArea(double minX, double minY, double maxX, double maxY,
			double increment) {

	}

	/**
	 * Prepare us for the next gcode command to come in.
	 */
	public void cleanup() {
		// move us to our target.
		delta = new Point3d();

		// save our gcode
		if (hasCode("G"))
			lastGCode = (int) getCodeValue("G");

		// clear our gcodes.
		codeValues.clear();
		seenCodes.clear();

		// empty comments
		comment = "";
	}
}
