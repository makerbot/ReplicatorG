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

import replicatorg.app.exceptions.BuildFailureException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.MachineModel;
import replicatorg.util.Point5d;

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
	 * Should we bypass the parser?
	 * @return true if this driver executes GCodes directly, false if the parser should be used to exercise it's interface. 
	 */
	public boolean isPassthroughDriver();
	
	/**
	 * Execute a line of GCode directly (ie, don't use the parser)
	 * @param code The line of GCode that we should execute
	 */
	public void executeGCodeLine(String code);

	/**
	 * are we finished with the last command?
	 */
	public boolean isFinished();

	/**
	 * Is our buffer empty? If don't have a buffer, its always true.
	 */
	public boolean isBufferEmpty();
	
	
	/**
	 * Check that the communication line is still up, the machine is still connected,
	 * and that the machine state is still good.
	 * TODO: Rename this? 
	 */
	public void assessState();
	
	/**
	 * Check if the device has reported an error
	 * @return True if there is an error waiting.
	 */
	public boolean hasError();

	/**
	 * Get a string message for the first driver error.
	 * @return
	 */
	public DriverError getError();
	
	/**
	 * do we have any errors? this method handles them.
	 */
	public void checkErrors() throws BuildFailureException;

	/**
	 * setup our driver for use.
	 */
	public void initialize() throws VersionException;
		
	/**
	 * uninitializes driver (disconnects from machine)
	 */
	public void uninitialize();

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
	 * get version information from the driver
	 */
	public String getDriverName(); // A human-readable name for the machine
									// type

	public String getFirmwareInfo();

	public Version getVersion();
	
	/** Called at regular intervals when under manual control. Allows insertion of 
	 * machine-specific logic into each manual control panel update. 
	 * @throws InterruptedException */
	public void updateManualControl();

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
	 * @throws RetryException 
	 */
	public void setCurrentPosition(Point5d p) throws RetryException;

	/** 
	 * Tell the machine to record it's current position into storage 
	 */
	public void storeHomePositions(EnumSet<AxisId> axes) throws RetryException;
	
	/** 
	 * Tell the machine to restore it's current position from storage 
	 */
	public void recallHomePositions(EnumSet<AxisId> axes) throws RetryException;
	
	/**
	 * @return true if the machine position is unknown
	 */
	public boolean positionLost();
	
	/**
	 * Get the current machine position
	 * @param update True if the driver should be forced to query the machine
	 * for its position, instead of using the cached value.
	 * @return
	 */
	public Point5d getCurrentPosition(boolean update);

	/**
	 * Indicate that the currently maintained position may no longer be the machine's position,
	 * and that the machine should be queried for its actual location.
	 */
	void invalidatePosition();

	/**
	 * Queue the next point to move to.
	 * @param p The location to move to, in mm.
	 * @throws RetryException 
	 */
	public void queuePoint(Point5d p) throws RetryException;

	public Point3d getOffset(int i);

	public void setOffsetX(int i, double j);

	public void setOffsetY(int i, double j);

	public void setOffsetZ(int i, double j);

	public Point5d getPosition();

	/**
	 * Tool methods
	 * @throws RetryException 
	 */
	public void requestToolChange(int toolIndex, int timeout) throws RetryException;

	public void selectTool(int toolIndex) throws RetryException;

	/**
	 * sets the feedrate in mm/minute
	 */
	public void setFeedrate(double feed);

	/**
	 * sets the feedrate in mm/minute
	 */
	public double getCurrentFeedrate();

	/**
	 * Home the given set of axes at the given feedrate.  If the feedrate is <=0, run at
	 * maximum feedrate for the appropriate axes.
	 * @throws RetryException 
	 */
	public void homeAxes(EnumSet<AxisId> axes, boolean positive, double feedrate) throws RetryException;

	/**
	 * delay / pause function
	 * @throws RetryException 
	 */
	public void delay(long millis) throws RetryException;

	/**
	 * functions for dealing with clamps
	 */
	public void openClamp(int clampIndex);

	public void closeClamp(int clampIndex);

	/**
	 * enabling/disabling our drivers (steppers, servos, etc.)
	 * @throws RetryException 
	 */
	public void enableDrives() throws RetryException;

	public void disableDrives() throws RetryException;

	/**
	 * enabling/disabling our drivers for individual axes. A disabled axis is
	 * generally able to move freely, while an enabled axis is clamped.
	 * @throws RetryException
	 */
	public void enableAxes(EnumSet<AxisId> axes) throws RetryException;
	
	public void disableAxes(EnumSet<AxisId> axes) throws RetryException;
	
	/**
	 * change our gear ratio
	 */
	public void changeGearRatio(int ratioIndex);
	
	public void readToolStatus();
	
	public int getToolStatus();

	/***************************************************************************
	 * Motor interface functions
	 **************************************************************************/
	public void setMotorDirection(int dir);

	public void setMotorRPM(double rpm) throws RetryException;

	public void setMotorSpeedPWM(int pwm) throws RetryException;

	public double getMotorRPM();

	public int getMotorSpeedPWM();

	/**
	 * Enable motor until stopped by disableMotor
	 * @throws RetryException 
	 */
	public void enableMotor() throws RetryException;

	/**
	 * Enable motor for a fixed duration, then disable
	 * @throws RetryException 
	 */
	public void enableMotor(long millis) throws RetryException;

	public void disableMotor() throws RetryException;

	/***************************************************************************
	 * Spindle interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setSpindleRPM(double rpm) throws RetryException;

	public void setSpindleSpeedPWM(int pwm) throws RetryException;

	public void setSpindleDirection(int dir);

	public double getSpindleRPM();

	public int getSpindleSpeedPWM();

	public void enableSpindle() throws RetryException;

	public void disableSpindle() throws RetryException;

	/***************************************************************************
	 * Temperature interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setTemperature(double temperature) throws RetryException;
	
	public void readTemperature();
	
	public double getTemperature();
	
	public double getTemperatureSetting();
	
	/***************************************************************************
	 * Platform Temperature interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setPlatformTemperature(double temperature) throws RetryException;
	
	public void readPlatformTemperature();
	
	public double getPlatformTemperature();

	public double getPlatformTemperatureSetting();

	/***************************************************************************
	 * Build chamber interface functions
	 **************************************************************************/
	public void setChamberTemperature(double temperature);
	
	public void readChamberTemperature();
	
	public double getChamberTemperature();

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
	 * @throws RetryException 
	 **************************************************************************/
	public void enableFan() throws RetryException;

	public void disableFan() throws RetryException;

	
	/***************************************************************************
	 * abp interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setAutomatedBuildPlatformRunning(boolean state) throws RetryException;
	
	/***************************************************************************
	 * Valve interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void openValve() throws RetryException;

	public void closeValve() throws RetryException;

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
	/** Stop the motion of the machine. A normal stop will merely halt all steppers.
	 * An abort (a stop with the abort bit set true) will also instruct the machine
	 * to stop all subsystems and toolhead.
	 */
	public void stop(boolean abort);

	public boolean hasSoftStop();

	public boolean hasEmergencyStop();
	
	public void reset();

	/***************************************************************************
	 * Heartbeat
	 **************************************************************************/
	public boolean heartbeat();

}
