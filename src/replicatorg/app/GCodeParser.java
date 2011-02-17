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

/* ___________________________________________________
 * @@@@@@@@@@@@@@@@@@@@@**^^""~~~"^@@^*@*@@**@@@@@@@@@
 * @@@@@@@@@@@@@*^^'"~   , - ' '; ,@@b. '  -e@@@@@@@@@
 * @@@@@@@@*^"~      . '     . ' ,@@@@(  e@*@@@@@@@@@@
 * @@@@@^~         .       .   ' @@@@@@, ~^@@@@@@@@@@@
 * @@@~ ,e**@@*e,  ,e**e, .    ' '@@@@@@e,  "*@@@@@'^@
 * @',e@@@@@@@@@@ e@@@@@@       ' '*@@@@@@    @@@'   0
 * @@@@@@@@@@@@@@@@@@@@@',e,     ;  ~^*^'    ;^~   ' 0
 * @@@@@@@@@@@@@@@^""^@@e@@@   .'           ,'   .'  @
 * @@@@@@@@@@@@@@'    '@@@@@ '         ,  ,e'  .    ;@
 * @@@@@@@@@@@@@' ,&&,  ^@*'     ,  .  i^"@e, ,e@e  @@
 * @@@@@@@@@@@@' ,@@@@,          ;  ,& !,,@@@e@@@@ e@@
 * @@@@@,~*@@*' ,@@@@@@e,   ',   e^~^@,   ~'@@@@@@,@@@
 * @@@@@@, ~" ,e@@@@@@@@@*e*@*  ,@e  @@""@e,,@@@@@@@@@
 * @@@@@@@@ee@@@@@@@@@@@@@@@" ,e@' ,e@' e@@@@@@@@@@@@@
 * @@@@@@@@@@@@@@@@@@@@@@@@" ,@" ,e@@e,,@@@@@@@@@@@@@@
 * @@@@@@@@@@@@@@@@@@@@@@@~ ,@@@,,0@@@@@@@@@@@@@@@@@@@
 * @@@@@@@@@@@@@@@@@@@@@@@@,,@@@@@@@@@@@@@@@@@@@@@@@@@
 * """""""""""""""""""""""""""""""""""""""""""""""""""
 * ~~~~~~~~~~~~WARNING: HERE BE DRAGONS ~~~~~~~~~~~~~~
 * 
 * Dragon from:
 * http://www.textfiles.com/artscene/asciiart/castles
 */

package replicatorg.app;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.Point3d;

import replicatorg.app.exceptions.GCodeException;
import replicatorg.app.exceptions.JobCancelledException;
import replicatorg.app.exceptions.JobEndException;
import replicatorg.app.exceptions.JobException;
import replicatorg.app.exceptions.JobRewindException;
import replicatorg.drivers.Driver;
import replicatorg.drivers.MultiTool;
import replicatorg.drivers.PenPlotter;
import replicatorg.drivers.RetryException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;


public class GCodeParser {
	// our driver we use.
	protected Driver driver;
	
	// The code that we are currently executing
	// TODO: do we actually need to keep this?
	GCode gcode;
	
	// Queue of points that we need to run. Yaay for dangerous state info!
	// TODO: Drop this!
	Queue< Point5d > pointQueue;
	
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

	protected Point5d target;
	protected Point5d delta;

	// false = incremental; true = absolute
	boolean absoluteMode = false;

	// our feedrate variables.
	/**
	 * Feedrate in mm/minute.
	 */
	double feedrate = 0.0;


	// current selected tool
	protected int tool = -1;

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
    protected Class<?> extClass;
    protected Object objExtClass;
	public static final String TB_CODE = "M";
	public static final int TB_INIT = 997;
	public static final int TB_MESSAGE = 998;
	public static final int TB_CLEANUP = 999;
	
	// Replicate old behavior of breaking out Z moves into seperate setTarget
	// calls.
	
	private boolean breakoutZMoves = Base.preferences.getBoolean("replicatorg.parser.breakzmoves", false);
	
	/**
	 * Creates the driver object.
	 */
	public GCodeParser() {
		// we default to millimeters
		units = UNITS_MM;
		curveSection = curveSectionMM;

		// setup our points.
//		current = new Point3d();
//		System.err.println("-CURRENT "+current.toString());
		target = new Point5d();
		delta = new Point5d();
		drillTarget = new Point3d();

		// init our offset
		currentOffset = new Point3d();
		
		// make a queue to store future point moves in
		pointQueue = new LinkedList< Point5d >();
	}

	/**
	 * Get the maximum feed rate from the driver's model.
	 */
	protected double getMaxFeedrate() {
		// TODO: right now this is defaulting to the x feedrate. We should
		// eventually check for z-axis motions and use that feedrate. We should
		// also either alter the model, or post a warning when the x and y
		// feedrates differ.
		return driver.getMachine().getMaximumFeedrates().x();
	}

	/**
	 * initialize parser with values from the driver
	 */
	public void init(Driver drv) {
		// our driver class
		driver = drv;
		// reload breakout
		breakoutZMoves = Base.preferences.getBoolean("replicatorg.parser.breakzmoves", false);
		// init our offset variables
		currentOffset = driver.getOffset(0);
		
		pointQueue = new LinkedList< Point5d >();
		
		// TODO: who uses this before it's initialized?
		gcode = new GCode("");
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
		
		gcode = new GCode(cmd);

		return true;
	}


	public double convertToMM(double value, int units) {
		if (units == UNITS_INCHES) {
			return value * 25.4;
		}
		return value;
	}

	public String getCommand() {
		return gcode.getCommand();
	}

	/**
	 * Actually execute the GCode we just parsed.
	 * @throws RetryException 
	 */
	public void execute() throws GCodeException, RetryException {
		// TODO: is this the proper way?
		// Set spindle speed?
		// if (gcode.hasCode('S'))
		// driver.setSpindleRPM(gcode.getCodeValue('S'));

		// TODO: This is a hack, fix it.
		// We have two states here- it is possible that the previous command
		// created a series of point motions, then encountered a retry.
		// If that is the case, then we should just keep trying to queue them.
		if (!pointQueue.isEmpty()) {
			while( !pointQueue.isEmpty()) {
				Base.logger.fine("dequeueing!");
				setTarget(pointQueue.peek());
				pointQueue.remove();
			}
		}
		else {
		
			// execute our other codes
			executeMCodes();
			executeGCodes();
	
			// Select our tool?
			int tempTool = (int) gcode.getCodeValue('T');
			if (gcode.hasCode('T')) {
				if (tempTool != tool)
					driver.selectTool(tempTool);
	
				tool = tempTool;
			}
		}
	}

	private void executeMCodes() throws GCodeException, RetryException {
		// find us an m code.
		if (gcode.hasCode('M')) {
			// If this machine handles multiple active toolheads, we always honor a T code
			// as being a annotation to send the given command to the given toolheads.  Be
			// aware that appending a T code to an M code will not necessarily generate a
			// change tool request!  Use M6 for that.
			// M6 was historically used to wait for toolheads to get up to temperature, so
			// you may wish to avoid using M6.
			if (gcode.hasCode('T') && driver instanceof MultiTool && ((MultiTool)driver).supportsSimultaneousTools()) {
				driver.getMachine().selectTool((int) gcode.getCodeValue('T'));
			}
			switch ((int) gcode.getCodeValue('M')) {
			// stop codes... handled by getStops();
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

			// tool change.
			case 6:
				int timeout = 65535;
				if (gcode.hasCode('P')) {
					timeout = (int)gcode.getCodeValue('P');
				}
				if (gcode.hasCode('T')) {
					driver.requestToolChange((int) gcode.getCodeValue('T'), timeout);
				}
				else {
					throw new GCodeException("The T parameter is required for tool changes. (M6)");
				}
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
				if (gcode.hasCode('Q'))
					driver.closeClamp((int) gcode.getCodeValue('Q'));
				else
					throw new GCodeException(
							"The Q parameter is required for clamp operations. (M10)");
				break;

			// open clamp
			case 11:
				if (gcode.hasCode('Q'))
					driver.openClamp((int) gcode.getCodeValue('Q'));
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
				break;
				// open collet
			case 22:
				driver.closeCollet();
				break;
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

			// read spindle speed
			case 50:
				driver.getSpindleRPM();
				break;
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
				if (gcode.hasCode('S'))
					driver.setTemperature(gcode.getCodeValue('S'));
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
				if (gcode.hasCode('S'))
					driver.setMotorSpeedPWM((int)Math.round(gcode.getCodeValue('S')));
				else if (gcode.hasCode('R'))
					driver.setMotorRPM(gcode.getCodeValue('R'));
				break;

			// set build platform temperature
			case 109:
				if (gcode.hasCode('S'))
					driver.setPlatformTemperature(gcode.getCodeValue('S'));
				break;

			// set build chamber temperature
			case 110:
				driver.setChamberTemperature(gcode.getCodeValue('S'));
				
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

			// Instruct the machine to store it's current position to EEPROM
			case 131:
			{
				EnumSet<AxisId> axes = EnumSet.noneOf(AxisId.class);

				if (gcode.hasCode('X')) axes.add(AxisId.X);
				if (gcode.hasCode('Y')) axes.add(AxisId.Y);
				if (gcode.hasCode('Z')) axes.add(AxisId.Z);
				if (gcode.hasCode('A')) axes.add(AxisId.A);
				if (gcode.hasCode('B')) axes.add(AxisId.B);
				
				driver.storeHomePositions(axes);
			}
				break;

			// Instruct the machine to restore it's current position from EEPROM
			case 132:
			{
				EnumSet<AxisId> axes = EnumSet.noneOf(AxisId.class);

				if (gcode.hasCode('X')) axes.add(AxisId.X);
				if (gcode.hasCode('Y')) axes.add(AxisId.Y);
				if (gcode.hasCode('Z')) axes.add(AxisId.Z);
				if (gcode.hasCode('A')) axes.add(AxisId.A);
				if (gcode.hasCode('B')) axes.add(AxisId.B);
				
				driver.recallHomePositions(axes);
			}
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
			
			// set servo 1 position
			case 300:
				if (gcode.hasCode('S')) {
					if (driver instanceof PenPlotter) {
						((PenPlotter)driver).setServoPos(0, gcode.getCodeValue('S'));
					}
				}
				break;

			// set servo 2 position
			case 301:
				if (gcode.hasCode('S')) {
					if (driver instanceof PenPlotter) {
						((PenPlotter)driver).setServoPos(1, gcode.getCodeValue('S'));
					}
				}
				break;
				
           case TB_INIT:
               // initialize extension
			// Syntax: M997 ClassName param,param,param,etc
			// your class needs to know how to process the params as it will just
			//receive a string.
			//This code uses a space delimiter
			//To do: should be more general purpose

               try {
                   String params[] = gcode.getCommand().split(" ");                       

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
                    String params[] = gcode.getCommand().split(" "); //method is param[1]
                    String params2[] = gcode.getCommand().split("\\'"); //params to pass are params2[1]

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
						+ (int) gcode.getCodeValue('M'));
			}
		}
	}

	private void executeGCodes() throws GCodeException, RetryException {
		// start us off at our current position...
		Point5d temp = driver.getCurrentPosition();

		// initialize our points, etc.
		double iVal = convertToMM(gcode.getCodeValue('I'), units); // / X offset
																// for arcs
		double jVal = convertToMM(gcode.getCodeValue('J'), units); // / Y offset
																// for arcs
		@SuppressWarnings("unused")
		double kVal = convertToMM(gcode.getCodeValue('K'), units); // / Z offset
																// for arcs
		@SuppressWarnings("unused")
		double qVal = convertToMM(gcode.getCodeValue('Q'), units); // / feed
																// increment for
																// G83
		double rVal = convertToMM(gcode.getCodeValue('R'), units); // / arc radius
		double xVal = convertToMM(gcode.getCodeValue('X'), units); // / X units
		double yVal = convertToMM(gcode.getCodeValue('Y'), units); // / Y units
		double zVal = convertToMM(gcode.getCodeValue('Z'), units); // / Z units
		double aVal = convertToMM(gcode.getCodeValue('A'), units); // / A units
		double bVal = convertToMM(gcode.getCodeValue('B'), units); // / B units
		// Note: The E axis is treated internally as the A or B axis
		double eVal = convertToMM(gcode.getCodeValue('E'), units); // / E units

		// adjust for our offsets
		xVal += currentOffset.x;
		yVal += currentOffset.y;
		zVal += currentOffset.z;

		// absolute just specifies the new position
		if (absoluteMode) {
			if (gcode.hasCode('X'))
				temp.setX(xVal);
			if (gcode.hasCode('Y'))
				temp.setY(yVal);
			if (gcode.hasCode('Z'))
				temp.setZ(zVal);
			if (gcode.hasCode('A'))
				temp.setA(aVal);
			if (gcode.hasCode('E')) {
				if (tool == 0)
					temp.setA(eVal);
				else if (tool == 1)
					temp.setB(eVal);
			}
			if (gcode.hasCode('B'))
				temp.setB(bVal);
		}
		// relative specifies a delta
		else {
			if (gcode.hasCode('X'))
				temp.setX(temp.x() + xVal);
			if (gcode.hasCode('Y'))
				temp.setY(temp.y() + yVal);
			if (gcode.hasCode('Z'))
				temp.setZ(temp.z() + zVal);
			if (gcode.hasCode('A'))
				temp.setA(temp.a() + aVal);
			if (gcode.hasCode('E')) {
				if (tool == 0)
					temp.setA(temp.a() + eVal);
				else if (tool == 1)
					temp.setB(temp.b() + eVal);
			}
			if (gcode.hasCode('B'))
				temp.setB(temp.b() + bVal);
		}

		// Get feedrate if supplied
		if (gcode.hasCode('F')) {
			// Read feedrate in mm/min.
			feedrate = gcode.getCodeValue('F');
			driver.setFeedrate(feedrate);
		}

		// did we get a gcode?
		if (gcode.hasCode('G')) {
			int gCode = (int) gcode.getCodeValue('G');

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
				// Note: We don't support 5D
				if (gcode.hasCode('I') || gcode.hasCode('J')) {
					// our centerpoint
					Point5d center = new Point5d();
					Point5d current = driver.getCurrentPosition();
					center.setX(current.x() + iVal);
					center.setY(current.y() + jVal);

					// Get the points for the arc
					if (gCode == 2)
						pointQueue.addAll(drawArc(center, temp, true));
					else
						pointQueue.addAll(drawArc(center, temp, false));
				}
				// or we want a radius based one
				else if (gcode.hasCode('R')) {
					Base.logger.warning("G02/G03 arcs with (R)adius parameter are not supported yet.");
					if (gCode == 2)
						pointQueue.addAll(drawRadius(temp, rVal, true));
					else
						pointQueue.addAll(drawRadius(temp, rVal, false));
				}
				
				// now play them back
				while( !pointQueue.isEmpty()) {
					setTarget(pointQueue.peek());
					pointQueue.remove();
				}
			}
				break;

			// dwell
			case 4:
				driver.delay((long) gcode.getCodeValue('P'));
				break;
			case 10:
				if (gcode.hasCode('P')) {
					int offsetSystemNum = ((int)gcode.getCodeValue('P'));
					if (offsetSystemNum >= 1 && offsetSystemNum <= 6) {
						if (gcode.hasCode('X')) driver.setOffsetX(offsetSystemNum, gcode.getCodeValue('X'));
						if (gcode.hasCode('Y')) driver.setOffsetY(offsetSystemNum, gcode.getCodeValue('Y'));
						if (gcode.hasCode('Z')) driver.setOffsetZ(offsetSystemNum, gcode.getCodeValue('Z'));
					}
				}
				else 
					Base.logger.warning("No coordinate system indicated use G10 Pn, where n is 0-6.");
				break;

			// plane selection codes
			case 17:
				currentPlane = XY_PLANE;
				break;
			case 18:
				//Base.logger.warning("ZX Plane moves are not supported yet.");
				currentPlane = ZX_PLANE;
				break;
			case 19:
				//Base.logger.warning("ZY Plane moves are not supported yet.");
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

			// This should be "return to home".  We need to introduce new GCodes for homing.
			case 28:
			{
				// home all axes?
				EnumSet<AxisId> axes = EnumSet.noneOf(AxisId.class);

				if (gcode.hasCode('X')) axes.add(AxisId.X);
				if (gcode.hasCode('Y')) axes.add(AxisId.Y);
				if (gcode.hasCode('Z')) axes.add(AxisId.Z);
				driver.homeAxes(axes, false, gcode.hasCode('F')?feedrate:0);
			}
				break;

			// New code: home negative.
			case 161:
			{
				// home all axes?
				EnumSet<AxisId> axes = EnumSet.noneOf(AxisId.class);

				if (gcode.hasCode('X')) axes.add(AxisId.X);
				if (gcode.hasCode('Y')) axes.add(AxisId.Y);
				if (gcode.hasCode('Z')) axes.add(AxisId.Z);
				driver.homeAxes(axes, false, gcode.hasCode('F')?feedrate:0);
			}
				break;

				// New code: home positive.
			case 162:
			{
				// home all axes?
				EnumSet<AxisId> axes = EnumSet.noneOf(AxisId.class);

				if (gcode.hasCode('X')) axes.add(AxisId.X);
				if (gcode.hasCode('Y')) axes.add(AxisId.Y);
				if (gcode.hasCode('Z')) axes.add(AxisId.Z);
				driver.homeAxes(axes, true, gcode.hasCode('F')?feedrate:0);
			}
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
					Point5d current = driver.getCurrentPosition();
					double minX = current.x();
					double minY = current.y();
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
				if (gcode.hasCode('X'))
					drillTarget.x = temp.x();
				if (gcode.hasCode('Y'))
					drillTarget.y = temp.y();
				if (gcode.hasCode('Z'))
					drillTarget.z = temp.z();
				if (gcode.hasCode('F'))
					drillFeedrate = gcode.getCodeValue('F');
				if (gcode.hasCode('R'))
					drillRetract = rVal;

				// set our vars for normal drilling
				if (gCode == 81) {
					drillDwell = 0;
					drillPecksize = 0.0;
				}
				// they want a dwell
				else if (gCode == 82) {
					if (gcode.hasCode('P'))
						drillDwell = (int) gcode.getCodeValue('P');
					drillPecksize = 0.0;
				}
				// fancy schmancy 'pecking' motion.
				else if (gCode == 83 || gCode == 183) {
					if (gcode.hasCode('P'))
						drillDwell = (int) gcode.getCodeValue('P');
					if (gcode.hasCode('Q'))
						drillPecksize = Math.abs(gcode.getCodeValue('Q'));

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

				Point5d current = driver.getCurrentPosition();

				if (gcode.hasCode('X'))
					current.setX(xVal);
				if (gcode.hasCode('Y'))
					current.setY(yVal);
				if (gcode.hasCode('Z'))
					current.setZ(zVal);
				if (gcode.hasCode('A'))
					current.setA(aVal);
				// Note: The E axis is treated internally as the A axis
				if (gcode.hasCode('E'))
					current.setA(eVal);
				if (gcode.hasCode('B'))
					current.setB(bVal);
				
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
				driver.setSpindleRPM((int) gcode.getCodeValue('S'));
				break;
				
			// error, error!
			default:
				throw new GCodeException("Unknown G code: G"
						+ (int) gcode.getCodeValue('G'));
			}
			
		}
	}

	/*
	 * drillTarget = new Point3d(); drillRetract = 0.0; drillFeedrate = 0.0;
	 * drillDwell = 0.0; drillPecksize = 0.0;
	 */
	private void drillingCycle(boolean speedPeck) throws RetryException {
		// Retract to R position if Z is currently below this
		Point5d current = driver.getCurrentPosition();
		if (current.z() < drillRetract) {
			driver.setFeedrate(getMaxFeedrate());
			setTarget(new Point5d(current.x(), current.y(), drillRetract, current.a(), current.b()));
		}

		// Move to start XY
		driver.setFeedrate(getMaxFeedrate());
		setTarget(new Point5d(drillTarget.x, drillTarget.y, current.z(), current.a(), current.b()));

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
				setTarget(new Point5d(drillTarget.x, drillTarget.y, targetZ, current.a(), current.b()));
			}

			// set our plunge depth
			targetZ -= deltaZ;
			// make sure we dont go too deep.
			if (targetZ < drillTarget.z)
				targetZ = drillTarget.z;

			// Move with controlled feed rate
			driver.setFeedrate(drillFeedrate);

			// do it!
			setTarget(new Point5d(drillTarget.x, drillTarget.y, targetZ, current.a(), current.b()));

			// Dwell if doing a G82
			if (drillDwell > 0)
				driver.delay(drillDwell);

			// Retract unless we're speed pecking.
			if (!speedPeck) {
				driver.setFeedrate(getMaxFeedrate());
				setTarget(new Point5d(drillTarget.x, drillTarget.y, drillRetract, current.a(), current.b()));
			}

		} while (targetZ > drillTarget.z);

		// double check for final speedpeck retract
		if (current.z() < drillRetract) {
			driver.setFeedrate(getMaxFeedrate());
			setTarget(new Point5d(drillTarget.x, drillTarget.y, drillRetract, current.a(), current.b()));
		}
	}

	// Note: 5D is not supported
	Queue< Point5d > drawArc(Point5d center, Point5d endpoint, boolean clockwise) {
		// System.out.println("Arc from " + current.toString() + " to " +
		// endpoint.toString() + " with center " + center);

		Queue< Point5d > points = new LinkedList< Point5d >();
		
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
		Point5d current = driver.getCurrentPosition();
		aX = current.x() - center.x();
		aY = current.y() - center.y();
		bX = endpoint.x() - center.x();
		bY = endpoint.y() - center.y();

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
		Point5d newPoint = new Point5d(current);
		double arcStartZ = current.z();
		for (s = 1; s <= steps; s++) {
			// Forwards for CCW, backwards for CW
			if (!clockwise)
				step = s;
			else
				step = steps - s;

			// calculate our waypoint.
			newPoint.setX(center.x() + radius * Math.cos(angleA + angle * ((double) step / steps)));
			newPoint.setY(center.y() + radius * Math.sin(angleA + angle * ((double) step / steps)));
			newPoint.setZ(arcStartZ + (endpoint.z() - arcStartZ) * s / steps);

			// start the move
			points.add(new Point5d(newPoint));
		}
		
		return points;
	}

	private Queue< Point5d > drawRadius(Point5d endpoint, double r, boolean clockwise) throws GCodeException {
		// not supported!
		throw new GCodeException("The drawRadius command is not supported");
	}

	private void setTarget(Point5d p) throws RetryException {
		// If you really want two separate moves, do it when you generate your
		// toolpath.
		// move z first
		Point5d current = driver.getCurrentPosition();
		if (breakoutZMoves) {
			if (p.z() != current.z()) {
				driver.queuePoint(new Point5d(current.x(), current.y(), p.z(), current.a(), current.b()));
			}
		}
		driver.queuePoint(new Point5d(p));
		current = new Point5d(p);
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
		String message = gcode.getComment();
		
		int mCode;

		if (gcode.hasCode('M')) {
			// we wanna do this after its finished whatever was before.
			driver.waitUntilBufferEmpty();

			mCode = (int) gcode.getCodeValue('M');

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
		delta = new Point5d();

		gcode = null;
	}
}
