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

package replicatorg.drivers;

import java.util.EnumSet;

import javax.vecmath.Point3d;

import org.w3c.dom.Node;

import replicatorg.app.GCodeParser;
import replicatorg.app.exceptions.BuildFailureException;
import replicatorg.app.exceptions.GCodeException;
import replicatorg.machine.model.Axis;
import replicatorg.machine.model.MachineModel;

public class DriverBaseImplementation implements Driver {
	// our gcode parser
	private GCodeParser parser;

	// models for our machine
	protected MachineModel machine;

	// our firmware version info
	private String firmwareName = "Unknown";

	protected Version version = new Version(0,0);
	protected Version preferredVersion = new Version(0,0);
	protected Version minimumVersion = new Version(0,0);
	
	// our point offsets
	private Point3d[] offsets;

	// are we initialized?
	private boolean isInitialized = false;

	// the length of our last move.
	private double moveLength = 0.0;

	// our error variable.
	private String error = "";

	// how fast are we moving in mm/minute
	private double currentFeedrate;

	// what is our mode of positioning?
	protected int positioningMode = 0;

	static public int ABSOLUTE = 0;

	static public int INCREMENTAL = 1;

	/**
	 * Creates the driver object.
	 */
	public DriverBaseImplementation() {
		// create our parser object
		parser = new GCodeParser();

		// initialize our offsets
		offsets = new Point3d[7];
		for (int i = 0; i < 7; i++)
			offsets[i] = new Point3d();

		// initialize our driver
		parser.init(this);

		// TODO: do this properly.
		machine = new MachineModel();
	}

	public void loadXML(Node xml) {
	}

	public void dispose() {
		// System.out.println("Disposing of driver.");
		parser = null;
	}

	/***************************************************************************
	 * Initialization handling functions
	 **************************************************************************/

	public void initialize() {
		setInitialized(true);
	}

	public void setInitialized(boolean status) {
		isInitialized = status;
	}

	public boolean isInitialized() {
		return isInitialized;
	}

	/***************************************************************************
	 * Error handling functions
	 **************************************************************************/

	protected void setError(String e) {
		error = e;
	}

	public void checkErrors() throws BuildFailureException {
		if (error.length() > 0)
			throw new BuildFailureException(error);
	}

	/***************************************************************************
	 * Parser handling functions
	 **************************************************************************/

	public void parse(String cmd) {
		// reset our values.
		moveLength = 0.0;

		parser.parse(cmd);
	}

	public GCodeParser getParser() {
		return parser;
	}

	public void execute() throws GCodeException, InterruptedException {
		assert (parser != null);
		parser.execute();
	}

	public boolean isFinished() {
		return true;
	}

	/***************************************************************************
	 * Firmware information functions
	 **************************************************************************/

	/**
	 * Is our buffer empty? If don't have a buffer, its always true.
	 */
	public boolean isBufferEmpty() {
		return true;
	}

	/**
	 * Wait until we've finished all commands.
	 */
	public void waitUntilBufferEmpty() {
		// sleep until we're empty.
		while (!isBufferEmpty()) {
			try {
				Thread.sleep(50);
			} catch (Exception e) {
			}
		}
	}

	/***************************************************************************
	 * Firmware information functions
	 **************************************************************************/

	public String getFirmwareInfo() {
		return firmwareName + " v" + getVersion();
	}

	public Version getVersion() {
		return version;
	}
	
	public Version getMinimumVersion() {
		return minimumVersion;
	}
	
	public Version getPreferredVersion() {
		return preferredVersion;
	}

	/***************************************************************************
	 * Machine positioning functions
	 **************************************************************************/

	public Point3d getOffset(int i) {
		return offsets[i];
	}

	private Point3d currentPosition = new Point3d();
	
	public void setCurrentPosition(Point3d p) {
		currentPosition = p;
	}

	/**
	 * Indicate that the currently maintained position may no longer be the machine's position,
	 * and that the machine should be queried for its actual location.
	 */
	public void invalidatePosition() {
		currentPosition = null;
	}
	
	/**
	 * Drivers should override this method to get the actual position as recorded by the machine.
	 * This is useful, for example, after stopping a print, to ask the machine where it is.
	 */
	protected Point3d reconcilePosition() {
		throw new RuntimeException("Position reconcilliation requested, but not implemented for this driver");
	}
	
	public Point3d getCurrentPosition() {
		if (currentPosition == null) {
			currentPosition = reconcilePosition();
		}
		return new Point3d(currentPosition);
	}

	public Point3d getPosition() {
		return getCurrentPosition();
	}

	public void queuePoint(Point3d p) {
		Point3d delta = getDelta(p);

		// calculate the length of each axis move
		double xFactor = Math.pow(delta.x, 2);
		double yFactor = Math.pow(delta.y, 2);
		double zFactor = Math.pow(delta.z, 2);

		// add to the total length
		moveLength += Math.sqrt(xFactor + yFactor + zFactor);

		// what is our feedrate?
		double feedrate = getSafeFeedrate(delta);

		// mostly for estimation driver.
		queuePoint(p, feedrate);

		// save it as our current position now.
		setCurrentPosition(p);
	}

	protected void queuePoint(Point3d p, Double feedrate) {
		// do nothing here.
	}

	public double getMoveLength() {
		return moveLength;
	}

	/**
	 * sets the feedrate in mm/minute
	 */
	public void setFeedrate(double feed) {
		currentFeedrate = feed;
	}

	/**
	 * gets the feedrate in mm/minute
	 */
	public double getCurrentFeedrate() {
		return currentFeedrate;
	}

	public double getSafeFeedrate(Point3d delta) {
		double feedrate = getCurrentFeedrate();

		Point3d maxFeedrates = machine.getMaximumFeedrates();

		// System.out.println("max feedrates: " + maxFeedrates);

		// no zero feedrates!
		if (feedrate == 0) {
			feedrate = Math.max(maxFeedrates.x, maxFeedrates.y);
			feedrate = Math.max(feedrate, maxFeedrates.z);
			feedrate = Math.max(feedrate, 1);
			// System.out.println("Zero feedrate!! " + feedrate);
		}

		if (delta.x != 0)
			feedrate = Math.min(feedrate, maxFeedrates.x);
		if (delta.y != 0)
			feedrate = Math.min(feedrate, maxFeedrates.y);
		if (delta.z != 0)
			feedrate = Math.min(feedrate, maxFeedrates.z);

		return feedrate;
	}

	public Point3d getDelta(Point3d p) {
		Point3d delta = new Point3d();
		Point3d current = getCurrentPosition();

		delta.x = Math.abs(p.x - current.x);
		delta.y = Math.abs(p.y - current.y);
		delta.z = Math.abs(p.z - current.z);

		return delta;
	}

	/***************************************************************************
	 * various homing functions
	 **************************************************************************/
	public void homeAxes(EnumSet<Axis> axes, boolean positive) {
	}

	/***************************************************************************
	 * Machine interface functions
	 **************************************************************************/
	public MachineModel getMachine() {
		return machine;
	}

	public void setMachine(MachineModel m) {
		machine = m;
	}

	/***************************************************************************
	 * Tool interface functions
	 **************************************************************************/
	public void requestToolChange(int toolIndex) {
		machine.selectTool(toolIndex);
	}

	public void selectTool(int toolIndex) {
		machine.selectTool(toolIndex);
	}

	/***************************************************************************
	 * pause function
	 **************************************************************************/
	public void delay(long millis) {
		// System.out.println("Delay: " + millis);
	}

	/***************************************************************************
	 * functions for dealing with clamps
	 **************************************************************************/
	public void openClamp(int index) {
		machine.getClamp(index).open();
	}

	public void closeClamp(int index) {
		machine.getClamp(index).close();
	}

	/***************************************************************************
	 * enabling/disabling our drivers (steppers, servos, etc.)
	 **************************************************************************/
	public void enableDrives() {
		machine.enableDrives();
	}

	public void disableDrives() {
		machine.disableDrives();
	}

	/***************************************************************************
	 * Change our gear ratio.
	 **************************************************************************/

	public void changeGearRatio(int ratioIndex) {
		machine.changeGearRatio(ratioIndex);
	}

	/***************************************************************************
	 * toolhead interface commands
	 **************************************************************************/

	/***************************************************************************
	 * Motor interface functions
	 **************************************************************************/
	public void setMotorDirection(int dir) {
		machine.currentTool().setMotorDirection(dir);
	}

	public void setMotorRPM(double rpm) {
		machine.currentTool().setMotorSpeedRPM(rpm);
	}

	public void setMotorSpeedPWM(int pwm) {
		machine.currentTool().setMotorSpeedPWM(pwm);
	}

	public void enableMotor() {
		machine.currentTool().enableMotor();
	}

	public void disableMotor() {
		machine.currentTool().disableMotor();
	}

	public double getMotorRPM() {
		return machine.currentTool().getMotorSpeedReadingRPM();
	}

	public int getMotorSpeedPWM() {
		return machine.currentTool().getMotorSpeedReadingPWM();
	}

	/***************************************************************************
	 * Spindle interface functions
	 **************************************************************************/
	public void setSpindleDirection(int dir) {
		machine.currentTool().setSpindleDirection(dir);
	}

	public void setSpindleRPM(double rpm) {
		machine.currentTool().setSpindleSpeedRPM(rpm);
	}

	public void setSpindleSpeedPWM(int pwm) {
		machine.currentTool().setSpindleSpeedPWM(pwm);
	}

	public void enableSpindle() {
		machine.currentTool().enableSpindle();
	}

	public void disableSpindle() {
		machine.currentTool().disableSpindle();
	}

	public double getSpindleRPM() {
		return machine.currentTool().getSpindleSpeedReadingRPM();
	}

	public int getSpindleSpeedPWM() {
		return machine.currentTool().getSpindleSpeedReadingPWM();
	}

	/***************************************************************************
	 * Temperature interface functions
	 **************************************************************************/
	public void setTemperature(double temperature) {
		machine.currentTool().setTargetTemperature(temperature);
	}

	public void readTemperature() {

	}

	public double getTemperature() {
		readTemperature();

		return machine.currentTool().getCurrentTemperature();
	}

	/***************************************************************************
	 * Platform Temperature interface functions
	 **************************************************************************/
	public void setPlatformTemperature(double temperature) {
		machine.currentTool().setPlatformTargetTemperature(temperature);
	}

	public void readPlatformTemperature() {

	}

	public double getPlatformTemperature() {
		readPlatformTemperature();

		return machine.currentTool().getPlatformCurrentTemperature();
	}

	/***************************************************************************
	 * Flood Coolant interface functions
	 **************************************************************************/
	public void enableFloodCoolant() {
		machine.currentTool().enableFloodCoolant();
	}

	public void disableFloodCoolant() {
		machine.currentTool().disableFloodCoolant();
	}

	/***************************************************************************
	 * Mist Coolant interface functions
	 **************************************************************************/
	public void enableMistCoolant() {
		machine.currentTool().enableMistCoolant();
	}

	public void disableMistCoolant() {
		machine.currentTool().disableMistCoolant();
	}

	/***************************************************************************
	 * Fan interface functions
	 **************************************************************************/
	public void enableFan() {
		machine.currentTool().enableFan();
	}

	public void disableFan() {
		machine.currentTool().disableFan();
	}

	/***************************************************************************
	 * Valve interface functions
	 **************************************************************************/
	public void openValve() {
		machine.currentTool().openValve();
	}

	public void closeValve() {
		machine.currentTool().closeValve();
	}

	/***************************************************************************
	 * Collet interface functions
	 **************************************************************************/
	public void openCollet() {
		machine.currentTool().openCollet();
	}

	public void closeCollet() {
		machine.currentTool().closeCollet();
	}

	/***************************************************************************
	 * Pause/unpause functionality for asynchronous devices
	 **************************************************************************/
	public void pause() {
		// No implementation needed for synchronous machines.
	}

	public void unpause() {
		// No implementation needed for synchronous machines.
	}

	/***************************************************************************
	 * Stop and system state reset
	 **************************************************************************/
	public void stop() {
		// No implementation needed for synchronous machines.
		System.err.println("Machine stop called.");
	}

	public void reset() {
		// No implementation needed for synchronous machines.
		System.err.println("Machine reset called.");
	}

	public String getDriverName() {
		return null;
	}
	
	public boolean heartbeat() {
		return true;
	}
	
	public boolean autoscan() {
		return false;
	}

	public double getChamberTemperature() {
		return 0;
	}

	public void readChamberTemperature() {
	}

	public void setChamberTemperature(double temperature) {
	}
}
