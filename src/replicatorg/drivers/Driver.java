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

package replicatorg.drivers;

import java.util.EnumSet;

import javax.vecmath.Point3d;

import org.w3c.dom.Node;

import replicatorg.app.GCodeParser;
import replicatorg.app.exceptions.BuildFailureException;
import replicatorg.app.exceptions.GCodeException;
import replicatorg.machine.model.Axis;
import replicatorg.machine.model.MachineModel;

// import org.xml.sax.*;
// import org.xml.sax.helpers.XMLReaderFactory;

public interface Driver {
	
	/**
	 * High level functions
	 */

	/**
	 * parse and load configuration data from XML
	 */
	public void loadXML(Node xml);

	/**
	 * parse a command. usually passes it through to the parser.
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
	 * Is our buffer empty? If don't have a buffer, its always true.
	 */
	public boolean isBufferEmpty();

	/**
	 * Wait until we've finished all commands.
	 */
	public void waitUntilBufferEmpty();

	/**
	 * do we have any errors? this method handles them.
	 */
	public void checkErrors() throws BuildFailureException;

	/**
	 * setup our driver for use.
	 */
	public void initialize();
	
	/**
	 * Autoscan for a working machine.
	 */
	public boolean autoscan();
	
	/**
	 * See if the driver has been successfully initialized.
	 * 
	 * @return true if the driver is initialized
	 */
	public boolean isInitialized();

	/**
	 * clean up the driver
	 */
	public void dispose();

	/***************************************************************************
	 * Machine interface functions
	 **************************************************************************/
	public MachineModel getMachine();

	public void setMachine(MachineModel m);

	/**
	 * execute the recently parsed GCode
	 * 
	 * @throws InterruptedException
	 */
	public void execute() throws GCodeException, InterruptedException;

	/**
	 * get version information from the driver
	 */
	public String getDriverName(); // A human-readable name for the machine
									// type

	public String getFirmwareInfo();

	public Version getVersion();

	public Version getMinimumVersion();
	
	public Version getPreferredVersion();
	
	/**
	 * Positioning Methods
	 */
	/**
	 * Tell the machine to consider its current position as being at p. Should
	 * not move the machine position.
	 * 
	 * @param p
	 *            the point to map the current position to
	 */
	public void setCurrentPosition(Point3d p);

	public Point3d getCurrentPosition();

	public void queuePoint(Point3d p);

	public Point3d getOffset(int i);

	public Point3d getPosition();

	/**
	 * Tool methods
	 */
	public void requestToolChange(int toolIndex);

	public void selectTool(int toolIndex);

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
	public void homeAxes(EnumSet<Axis> axes);

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

	/***************************************************************************
	 * Motor interface functions
	 **************************************************************************/
	public void setMotorDirection(int dir);

	public void setMotorRPM(double rpm);

	public void setMotorSpeedPWM(int pwm);

	public double getMotorRPM();

	public int getMotorSpeedPWM();

	public void enableMotor();

	public void disableMotor();

	/***************************************************************************
	 * Spindle interface functions
	 **************************************************************************/
	public void setSpindleRPM(double rpm);

	public void setSpindleSpeedPWM(int pwm);

	public void setSpindleDirection(int dir);

	public double getSpindleRPM();

	public int getSpindleSpeedPWM();

	public void enableSpindle();

	public void disableSpindle();

	/***************************************************************************
	 * Temperature interface functions
	 **************************************************************************/
	public void setTemperature(double temperature);

	public void readTemperature();

	public double getTemperature();

	/***************************************************************************
	 * Flood Coolant interface functions
	 **************************************************************************/
	public void enableFloodCoolant();

	public void disableFloodCoolant();

	/***************************************************************************
	 * Mist Coolant interface functions
	 **************************************************************************/
	public void enableMistCoolant();

	public void disableMistCoolant();

	/***************************************************************************
	 * Fan interface functions
	 **************************************************************************/
	public void enableFan();

	public void disableFan();

	/***************************************************************************
	 * Valve interface functions
	 **************************************************************************/
	public void openValve();

	public void closeValve();

	/***************************************************************************
	 * Collet interface functions
	 **************************************************************************/
	public void openCollet();

	public void closeCollet();

	/***************************************************************************
	 * Pause/unpause functionality for asynchronous devices
	 **************************************************************************/
	public void pause();

	public void unpause();

	/***************************************************************************
	 * Stop and system state reset
	 **************************************************************************/
	public void stop();

	public void reset();

	/***************************************************************************
	 * Heartbeat
	 **************************************************************************/
	public boolean heartbeat();

}
