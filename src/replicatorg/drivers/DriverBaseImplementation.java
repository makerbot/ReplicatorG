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

import java.awt.Color;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.vecmath.Point3d;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.exceptions.BuildFailureException;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.MachineModel;
import replicatorg.util.Point5d;

public abstract class DriverBaseImplementation implements Driver, DriverQueryInterface{
	// models for our machine
	protected MachineModel machine;

	// our firmware version info
	private String firmwareName = "Unknown";
	/// the 'proper name' of our bot. null indicates it is not yet read, or read failed
	protected String botName = null;

	
	protected Version version = new Version(0,0);
	protected Version preferredVersion = new Version(0,0);
	protected Version minimumVersion = new Version(0,0);
	
	// our point offsets
	private Point3d[] offsets;

	// are we initialized?
	private AtomicBoolean isInitialized = new AtomicBoolean(false);

	// our error variable.
	private ConcurrentLinkedQueue<DriverError> errorList;

	// how fast are we moving in mm/minute
	private double currentFeedrate;

	/**
	 * Support for emergency stop is not assumed until it is detected. Detection of this feature should be in initialization.
	 */
	protected boolean hasEmergencyStop = false;
	
	/**
	 * Support for soft stop (e.g. for continuous jog) is not assumed until it is detected. Detection of this feature should be in initialization.
	 */
	protected boolean hasSoftStop = false;
	
	/**
	 * Creates the driver object.
	 */
	protected DriverBaseImplementation() {
		errorList = new ConcurrentLinkedQueue<DriverError>();

		// initialize our offsets
		offsets = new Point3d[7];
		for (int i = 0; i < 7; i++)
			offsets[i] = new Point3d();  // Constructs and initializes a Point3d to (0,0,0)

		// TODO: do this properly.
		machine = new MachineModel();
	}	
	
	protected int fixToolIndex(int toolhead) {
		if (toolhead < 0) {
			new IllegalStateException("Negative toolhead index: " + toolhead)
					.printStackTrace();
			return machine.currentTool().getIndex();
		} else {
			return toolhead;
		}
	}

	@Override public void loadXML(Node xml) {
	}
	
	@Override public void updateManualControl() {
	}
	
	@Override public boolean isPassthroughDriver() {
		return false;
	}
	
	/**
	 * Execute a line of GCode directly (ie, don't use the parser)
	 * @param code The line of GCode that we should execute
	 */
	@Override public void executeGCodeLine(String code) {
		Base.logger.severe("Ignoring executeGCode command: " + code);
	}

	@Override public void dispose() {
		if (Base.logger.isLoggable(Level.FINE)) {
			Base.logger.fine("Disposing of driver " + getDriverName());
		}
	}

	/***************************************************************************
	 * Initialization handling functions
	 **************************************************************************/

	@Override public void initialize() {
		setInitialized(true);
	}

	@Override public void uninitialize() {
		setInitialized(false);
	}

	protected void setInitialized(boolean status) {
		synchronized(isInitialized)
		{
			isInitialized.set(status);
			if (!status) { invalidatePosition(); }
		}
	}

	@Override public boolean isInitialized() {
		return isInitialized.get();
	}

	/***************************************************************************
	 * Error handling functions
	 **************************************************************************/

	@Override public void assessState() {
	}
	
	protected void setError(DriverError newError) {
		errorList.add(newError);
	}
	
	protected void setError(String e) {
		setError(new DriverError(e, true));
	}

	
	@Override public boolean hasError() {
		return (errorList.size() > 0);
	}
	
	@Override public DriverError getError() {
		return errorList.remove();
	}

	@Override @Deprecated
	public void checkErrors() throws BuildFailureException {
		if (errorList.size() > 0) {
			throw new BuildFailureException(getError().getMessage());
		}
	}


	@Override public boolean isFinished() {
		return true;
	}

	/***************************************************************************
	 * Firmware information functions
	 **************************************************************************/

	/**
	 * Is our buffer empty? If don't have a buffer, its always true.
	 */
	@Override public boolean isBufferEmpty() {
		return true;
	}

	/***************************************************************************
	 * Firmware information functions
	 **************************************************************************/

	@Override public String getFirmwareInfo() {
		return firmwareName + " v" + getVersion();
	}
	
	@Override public Version getVersion() {
		return version;
	}
	
	@Override public Version getMinimumVersion() {
		return minimumVersion;
	}
	
	@Override public Version getPreferredVersion() {
		return preferredVersion;
	}

	/***************************************************************************
	 * Machine positioning functions
	 **************************************************************************/

	@Override public Point3d getOffset(int i) {
		return offsets[i];
	}

	@Override public void setOffsetX(int offsetSystemNum, double j) {
		offsets[offsetSystemNum].x = j;
	}

	@Override public void setOffsetY(int offsetSystemNum, double j) {
		offsets[offsetSystemNum].y = j;
	}

	@Override public void setOffsetZ(int offsetSystemNum, double j) {
		offsets[offsetSystemNum].z = j;
	}

	protected final AtomicReference<Point5d> currentPosition =
		new AtomicReference<Point5d>(null);
	
	@Override public void setCurrentPosition(Point5d p) throws RetryException {
		setInternalPosition(p);
	}

	/**
	 * Indicate that the currently maintained position may no longer be the machine's position,
	 * and that the machine should be queried for its actual location.
	 */
	@Override public void invalidatePosition() {
		setInternalPosition(null);
	}
	
	/**
	 * Drivers should override this method to get the actual position as recorded by the machine.
	 * This is useful, for example, after stopping a print, to ask the machine where it is.
	 */
	protected Point5d reconcilePosition() throws RetryException {
		throw new RuntimeException("Position reconcilliation requested, but not implemented for this driver");
	}
	
	/**
	 * @return true if the machine position is unknown
	 */
	@Override public boolean positionLost() {
		return (currentPosition.get() == null);
	}
	
	/** 
	 * Gets the current machine position. If forceUpdate is false, then the cached position is returned if available,
	 * otherwise the machine is polled for it's current position.
	 * 
	 * If a valid position can be determined, then it is returned. Otherwise, a zero position is returned.
	 * 
	 * Side effects: currentPosition will be updated with the current position if the machine position is successfully polled.
	 */
	@Override public Point5d getCurrentPosition(boolean forceUpdate) {
		synchronized(currentPosition)
		{
			// If we are lost, or an explicit update has been requested, poll the machine for it's state. 
			if (positionLost() || forceUpdate) {
				try {
					// Try to reconcile our position. 
					Point5d newPoint = reconcilePosition();
					setInternalPosition(newPoint);
					
				} catch (RetryException e) {
					Base.logger.severe("Attempt to reconcile machine position failed, due to Retry Exception");
				}
			}
			
			// If we are still lost, just return a zero position.
			if (positionLost()) {
				return new Point5d();
			}
			
			return new Point5d(currentPosition.get());
		}
	}

	@Override public Point5d getPosition() {
		return getCurrentPosition(false);
	}

	/**
	 * Queue the given point.
	 * @param p The point, in mm.
	 * @throws RetryException 
	 */
	@Override public void queuePoint(Point5d p) throws RetryException {
		setInternalPosition(p);
	}

	protected void setInternalPosition(Point5d position) {
		currentPosition.set(position);
	}
	
	/**
	 * sets the feedrate in mm/minute
	 */
	@Override public void setFeedrate(double feed) {
		currentFeedrate = feed;
	}

	/**
	 * gets the feedrate in mm/minute
	 */
	@Override public double getCurrentFeedrate() {
		return currentFeedrate;
	}

	/**
	 * Return the maximum safe feedrate, given in mm/min., for the given delta and current feedrate.
	 * @param delta The delta in mm.
	 * @return safe feedrate in mm/min
	 */
	protected double getSafeFeedrate(Point5d delta) {
		double feedrate = getCurrentFeedrate();

		Point5d maxFeedrates = machine.getMaximumFeedrates();

		// If the current feedrate is 0, set it to the maximum feedrate of any
		// of the machine axis. If those are also all 0 (misconfiguration?),
		// set the feedrate to 1.
		// TODO: Where else is feedrate set?
		if (feedrate == 0) {
			for (int i=0;i<5;i++) {
				feedrate = Math.max(feedrate, maxFeedrates.get(i));
			}
			feedrate = Math.max(feedrate, 1);
			Base.logger.warning("Zero feedrate detected, reset to: " + feedrate);
		}

		// Determine the magnitude of this delta
		double length = delta.length();
		
		// For each axis: if the current feedrate will cause this axis to move
		// faster than it's maximum feedrate, lower the system feedrate so
		// that it will be compliant.
		for (int i=0;i<5;i++) {
			if (delta.get(i) != 0) {
				if (feedrate * delta.get(i) / length > maxFeedrates.get(i)) {
					feedrate = maxFeedrates.get(i) * length / delta.get(i);
				}
			}
		}
		
		// Return the feedrate, which is how fast the toolhead will be moving (magnitude of the toolhead velocity)
		return feedrate;
	}

	protected Point5d getDelta(Point5d p) {
		Point5d delta = new Point5d();
		Point5d current = getCurrentPosition(false);

		delta.sub(p, current); // delta = p - current
		delta.absolute(); // absolute value of each component

		return delta;
	}

	/***************************************************************************
	 * various homing functions
	 * @throws RetryException 
	 **************************************************************************/
	@Override public void homeAxes(EnumSet<AxisId> axes, boolean positive, double feedrate) throws RetryException {
	}

	/***************************************************************************
	 * Machine interface functions
	 **************************************************************************/
	@Override public MachineModel getMachine() {
		return machine;
	}

	@Override public void setMachine(MachineModel m) {
		machine = m;
	}

	/***************************************************************************
	 * Tool interface functions
	 * @throws RetryException 
	 **************************************************************************/
	@Override public void requestToolChange(int toolIndex, int timeout) throws RetryException {
		machine.selectTool(toolIndex);
	}

	@Override public void selectTool(int toolIndex) throws RetryException {
		machine.selectTool(toolIndex);
	}
	
	@Override public int getCurrentToolIndex() {
		return machine.currentTool().getIndex();
	}

	/***************************************************************************
	 * pause function
	 * @throws RetryException 
	 **************************************************************************/
	@Override public void delay(long millis) throws RetryException {
	}


	/***************************************************************************
	 * functions for dealing with clamps
	 **************************************************************************/
	@Override public void openClamp(int index) {
		machine.getClamp(index).open();
	}

	@Override public void closeClamp(int index) {
		machine.getClamp(index).close();
	}

	/***************************************************************************
	 * enabling/disabling our drivers (steppers, servos, etc.)
	 * @throws RetryException 
	 **************************************************************************/
	@Override public void enableDrives() throws RetryException {
		machine.enableDrives();
	}

	@Override public void disableDrives() throws RetryException {
		machine.disableDrives();
	}

	@Override public void enableAxes(EnumSet<AxisId> axes) throws RetryException {
		// Not all drivers support this method.
	}
	
	@Override public void disableAxes(EnumSet<AxisId> axes) throws RetryException {
		// Not all drivers support this method.
	}

	/***************************************************************************
	 * Change our gear ratio.
	 **************************************************************************/

	@Override public void changeGearRatio(int ratioIndex) {
		machine.changeGearRatio(ratioIndex);
	}

	/***************************************************************************
	 * toolhead interface commands
	 **************************************************************************/

	/***************************************************************************
	 * Motor interface functions
	 **************************************************************************/
	@Override
	public void setMotorDirection(int dir, int toolhead) {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).setMotorDirection(dir);		
	}


	@Override
	public void setMotorRPM(double rpm, int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).setMotorSpeedRPM(rpm);

	}
	
	@Override public void setMotorSpeedPWM(int pwm, int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).setMotorSpeedPWM(pwm);
	}

	@Override
	public void enableMotor(long millis, int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		enableMotor(toolhead);
		delay( millis );
		disableMotor(toolhead);
	}
	
	@Override
	public void enableMotor(int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).enableMotor();
		
	}
	
	@Override public void disableMotor(int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).disableMotor();
	}

	@Override public double getMotorRPM(int toolhead) {
		toolhead = fixToolIndex(toolhead);
		return machine.getTool(toolhead).getMotorSpeedRPM();
	}

	@Override public int getMotorSpeedPWM(int toolhead) {
		toolhead = fixToolIndex(toolhead);
		return machine.getTool(toolhead).getMotorSpeedPWM();
	}

	public double getMotorSteps() {
		return machine.currentTool().getMotorSteps();
	}

	@Override public void readToolStatus(int toolhead) {
		
	}
	
	@Override public int getToolStatus(int toolhead) {
		toolhead = fixToolIndex(toolhead);
		readToolStatus(toolhead);

		return machine.getTool(toolhead).getToolStatus();
	}
	
	/***************************************************************************
	 * Spindle interface functions
	 **************************************************************************/
	@Override public void setSpindleDirection(int dir, int toolhead) {
		toolhead = fixToolIndex(toolhead);
		machine.getTool(toolhead).setSpindleDirection(dir);
	}
	
	@Override public void setSpindleRPM(double rpm, int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).setSpindleSpeedRPM(rpm);
	}

	@Override public void setSpindleSpeedPWM(int pwm, int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).setSpindleSpeedPWM(pwm);
	}

	@Override public void enableSpindle(int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).enableSpindle();
	}

	@Override public void disableSpindle(int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).disableSpindle();
	}
	
	@Override public double getSpindleRPM(int toolhead) {
		if (toolhead == -1) toolhead = machine.currentTool().getIndex();
		return machine.getTool(toolhead).getSpindleSpeedReadingRPM();
	}

	@Override public int getSpindleSpeedPWM(int toolhead) {
		if (toolhead == -1) toolhead = machine.currentTool().getIndex();
		return machine.currentTool().getSpindleSpeedReadingPWM();
	}
	
	/***************************************************************************
	 * Temperature interface functions
	 * @throws RetryException 
	 **************************************************************************/

	@Override
	public void setTemperature(double temperature, int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).setTargetTemperature(temperature);
	}

	@Override public void readTemperature(int toolhead) {

	}

	@Override
	public double getTemperature(int toolhead) {
		toolhead = fixToolIndex(toolhead);

		return machine.getTool(toolhead).getCurrentTemperature();
	}

	/***************************************************************************
	 * Platform Temperature interface functions
	 * @throws RetryException 
	 **************************************************************************/
	@Override
	public void setPlatformTemperature(double temperature, int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);

		machine.getTool(toolhead).setPlatformTargetTemperature(temperature);
	}

	@Override public void readPlatformTemperature(int toolhead) {
		toolhead = fixToolIndex(toolhead);
	
	}

	@Override public double getPlatformTemperature(int toolhead) {
		toolhead = fixToolIndex(toolhead);

		readPlatformTemperature(toolhead);
		return machine.getTool(toolhead).getPlatformCurrentTemperature();
	}

	/***************************************************************************
	 * Flood Coolant interface functions
	 **************************************************************************/
	@Override public void enableFloodCoolant() {
		machine.currentTool().enableFloodCoolant();
	}

	@Override public void disableFloodCoolant() {
		machine.currentTool().disableFloodCoolant();
	}

	/***************************************************************************
	 * Mist Coolant interface functions
	 **************************************************************************/
	@Override public void enableMistCoolant() {
		machine.currentTool().enableMistCoolant();
	}

	@Override public void disableMistCoolant() {
		machine.currentTool().disableMistCoolant();
	}

	/***************************************************************************
	 * Fan interface functions
	 * @throws RetryException 
	 **************************************************************************/
	@Override
	public void enableFan(int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);
		machine.getTool(toolhead).enableFan();
	}


	@Override
	public void disableFan(int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);
		machine.getTool(toolhead).disableFan();
	}
	
	@Override
	public void setAutomatedBuildPlatformRunning(boolean state, int toolhead)
			throws RetryException {
		toolhead = fixToolIndex(toolhead);
		machine.getTool(toolhead).setAutomatedBuildPlatformRunning(state);
	}

	
	@Override public boolean hasAutomatedBuildPlatform(int toolhead)
	{
		toolhead = fixToolIndex(toolhead);
		return machine.getTool(toolhead).hasAutomatedPlatform();
	}

	/***************************************************************************
	 * Valve interface functions
	 * @throws RetryException 
	 **************************************************************************/
	@Override public void openValve(int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);
		machine.getTool(toolhead).openValve();
	}

	@Override public void closeValve(int toolhead) throws RetryException {
		toolhead = fixToolIndex(toolhead);
		machine.getTool(toolhead).closeValve();
	}

	@Override public void setStepperVoltage(int stepperId, int referenceValue) throws RetryException
	{
		Base.logger.fine("BaseImplementation setStepperVoltage called.");
	}
	
	@Override public int getStepperVoltage(int stepperId)
	{
		Base.logger.fine("BaseImplementation getStepperVoltage called.");
		return -1;
	}

	
	@Override public void setLedStrip(Color color, int effectId) throws RetryException 
	{
		Base.logger.fine("BaseImplementation setLedStrip called.");
	}
	
	
	@Override public void sendBeep(int frequencyHz, int durationMs,int effect) throws RetryException
	{
		Base.logger.fine("BaseImplementation sendBeep called.");
	}

	/***************************************************************************
	 * Collet interface functions
	 **************************************************************************/
	@Override public void openCollet() {
		machine.currentTool().openCollet();
	}

	@Override public void closeCollet() {
		machine.currentTool().closeCollet();
	}

	/***************************************************************************
	 * Pause/unpause functionality for asynchronous devices
	 **************************************************************************/
	@Override public void pause() {
		// No implementation needed for synchronous machines.
	}

	@Override public void unpause() {
		// No implementation needed for synchronous machines.
	}

	/***************************************************************************
	 * Stop and system state reset
	 **************************************************************************/
	@Override public void stop(boolean abort) {
		// No implementation needed for synchronous machines.
		Base.logger.info("Machine stop called.");
	}

	@Override public void reset() {
		// No implementation needed for synchronous machines.
		Base.logger.info("Machine reset called.");
	}

	@Override public String getDriverName() {
		return null;
	}
	
	@Override public boolean heartbeat() {
		return true;
	}
	
	@Override public double getChamberTemperature() {
		return 0;
	}

	@Override public void readChamberTemperature() {
	}

	@Override public void setChamberTemperature(double temperature) {
	}

	@Override public double getPlatformTemperatureSetting(int toolhead) {
		if (toolhead == -1) toolhead = machine.currentTool().getIndex();
		return machine.getTool(toolhead).getPlatformTargetTemperature();
	}

	@Override public double getTemperatureSetting(int toolhead) {
		if (toolhead == -1) toolhead = machine.currentTool().getIndex();
		return machine.getTool(toolhead).getTargetTemperature();
	}

	@Override public void storeHomePositions(EnumSet<AxisId> axes) throws RetryException {
	}

	@Override public void recallHomePositions(EnumSet<AxisId> axes) throws RetryException {
	}

	@Override public boolean hasSoftStop() {

		return hasSoftStop;
	}

	@Override public boolean hasEmergencyStop() {
		return hasEmergencyStop;
	}

	@Override
	public Point5d getMaximumFeedrates() {
		return (getMachine().getMaximumFeedrates());
	}

	@Override
	public void readAllTemperatures() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readAllPlatformTemperatures() {
		// TODO Auto-generated method stub
		
	}


}
