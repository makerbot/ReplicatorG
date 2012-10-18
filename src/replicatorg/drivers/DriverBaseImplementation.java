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

public class DriverBaseImplementation implements Driver, DriverQueryInterface{
//	// our gcode parser
//	private GCodeParser parser;

	// models for our machine
	protected MachineModel machine;

	// our firmware version info
	private String firmwareName = "Unknown";
	/// the 'proper name' of our bot. null indicates it is not yet read, or read failed
	protected String botName = null;

	
	protected Version version = new Version(0,0);
	protected Version preferredVersion = new Version(0,0);
	protected Version minimumVersion = new Version(0,0);
  protected Version minimumAccelerationVersion = new Version(0,0);
  protected Version minimumJettyAccelerationVersion = new Version(0,0);
  protected Version minAdvancedFeatureVersion = new Version(0,0);    
	
	// our point offsets
	protected Point3d[] offsets;

	// are we initialized?
	private AtomicBoolean isInitialized = new AtomicBoolean(false);

	private int buildToFileVersion = 0;

	// our error variable.
	ConcurrentLinkedQueue<DriverError> errorList;

	// how fast are we moving in mm/minute
	private double currentFeedrateMM;

	// what is our mode of positioning?
	protected int positioningMode = 0;

	static public int ABSOLUTE = 0;

	static public int INCREMENTAL = 1;

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
	public DriverBaseImplementation() {
		errorList = new ConcurrentLinkedQueue<DriverError>();

		// initialize our offsets
		offsets = new Point3d[7];
		for (int i = 0; i < 7; i++)
			offsets[i] = new Point3d();  // Constructs and initializes a Point3d to (0,0,0)

		// TODO: do this properly.
		machine = new MachineModel();
	}	
	
	public void loadXML(Node xml) {
	}
	
	public void updateManualControl() {
	}
	
	public boolean isPassthroughDriver() {
		return false;
	}
	
	/**
	 * Execute a line of GCode directly (ie, don't use the parser)
	 * @param code The line of GCode that we should execute
	 */
	public void executeGCodeLine(String code) {
		Base.logger.severe("Ignoring executeGCode command: " + code);
	}

	public void dispose() {
		if (Base.logger.isLoggable(Level.FINE)) {
			Base.logger.fine("Disposing of driver " + getDriverName());
		}
//		parser = null;
	}

	/***************************************************************************
	 * Initialization handling functions
	 **************************************************************************/

	public void initialize() {
		setInitialized(true);
	}

	public void uninitialize() {
		setInitialized(false);
	}

	public void setInitialized(boolean status) {
		synchronized(isInitialized)
		{
			isInitialized.set(status);
			if (!status) { invalidatePosition(); }
		}
	}

	public boolean isInitialized() {
		return isInitialized.get();
	}

	/***************************************************************************
	 * Error handling functions
	 **************************************************************************/

	public void assessState() {
	}
	
	protected void setError(DriverError newError) {
		errorList.add(newError);
	}
	
	protected void setError(String e) {
		setError(new DriverError(e, true));
	}

	
	public boolean hasError() {
		return (errorList.size() > 0);
	}
	
	public DriverError getError() {
		return errorList.remove();
	}

	@Deprecated
	public void checkErrors() throws BuildFailureException {
		if (errorList.size() > 0) {
			throw new BuildFailureException(getError().getMessage());
		}
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

	/***************************************************************************
	 * Firmware information functions
	 **************************************************************************/

	public void setFirmwareName(String firmware)
	{
		firmwareName = firmware;
	}
	
	public String getFirmwareInfo() {
		return firmwareName + " v" + getVersion();
	}
	
	public String getBotName(){
		if ( botName != null ) 
			return botName;
		return "Unnamed Bot (a Sad Bot)";
	}

	public Version getVersion() {
		return version;
	}
	
	public Version getMinimumVersion() {
		return minimumVersion;
	}

  public Version getMinimumAccelerationVersion(){
          return minimumAccelerationVersion;
  }

  public Version getMinimumJettyAccelerationVersion(){
          return minimumJettyAccelerationVersion;
  }

  public Version getMinimumAdvancedFeatureVersion(){
          return minAdvancedFeatureVersion;
  }
	public Version getPreferredVersion() {
		return preferredVersion;
	}

	//Return s3g/x3g version (3 or 4)
	//Set when build is called, if value = 0
	//No version has been set(not saving to file)
	public int getBuildToFileVersion() {
		return buildToFileVersion;
	}

	//Sets verision s3g/x3g (3 or 4)
	public void setBuildToFileVersion(int version){
		buildToFileVersion = version;
	}

	/***************************************************************************
	 * Machine positioning functions
	 **************************************************************************/

	public Point3d getOffset(int i) {
		return offsets[i];
	}

	public void setOffsetX(int offsetSystemNum, double j) {
		offsets[offsetSystemNum].x = j;
	}

	public void setOffsetY(int offsetSystemNum, double j) {
		offsets[offsetSystemNum].y = j;
	}

	public void setOffsetZ(int offsetSystemNum, double j) {
		offsets[offsetSystemNum].z = j;
	}

	protected final AtomicReference<Point5d> currentPosition =
		new AtomicReference<Point5d>(null);
	
	public void setCurrentPosition(Point5d p) throws RetryException {
		currentPosition.set(p);
	}

	/**
	 * Indicate that the currently maintained position may no longer be the machine's position,
	 * and that the machine should be queried for its actual location.
	 */
	public void invalidatePosition() {
//		System.err.println("invalidating position.");
		currentPosition.set(null);
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
	public boolean positionLost() {
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
	public Point5d getCurrentPosition(boolean forceUpdate) {
		synchronized(currentPosition)
		{
			// If we are lost, or an explicit update has been requested, poll the machine for it's state. 
			if (positionLost() || forceUpdate) {
				try {
					// Try to reconcile our position. 
					Point5d newPoint = reconcilePosition();
					currentPosition.set(newPoint);
					
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

	public Point5d getPosition() {
		return getCurrentPosition(false);
	}

	/**
	 * Queue the given point.
	 * @param p The point, in mm.
	 * @throws RetryException 
	 */
	public void queuePoint(Point5d p) throws RetryException {
		setInternalPosition(p);
	}

	protected void setInternalPosition(Point5d position) {
		currentPosition.set(position);
	}
	
	/**
	 * sets the feedrate in mm/minute
	 */
	public void setFeedrateMM(double feed) {
		currentFeedrateMM = feed;
	}

	/**
	 * gets the feedrate in mm/minute
	 */
	public double getCurrentFeedrate() {
		return currentFeedrateMM;
	}

	/**
	 * Return the maximum safe feedrate, given in mm/min., for the given delta and current feedrate.
	 * @param delta The delta in mm.
	 * @return safe feedrate in mm/min
	 */
	public double getSafeFeedrate(Point5d delta) {
		double feedrate = getCurrentFeedrate();

		Point5d maxFeedrates = machine.getMaximumFeedrates();

		// System.out.println("max feedrates: " + maxFeedrates);

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

	public Point5d getDelta(Point5d p) {
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
	public void homeAxes(EnumSet<AxisId> axes, boolean positive, double feedrate) throws RetryException {
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
	 * @throws RetryException 
	 **************************************************************************/
	public void requestToolChange(int toolIndex, int timeout) throws RetryException {
		machine.selectTool(toolIndex);
	}

	public void selectTool(int toolIndex) throws RetryException {
		machine.selectTool(toolIndex);
	}
	
	/***************************************************************************
	 * Acceleration interface functions
	 * @throws RetryException 
	 **************************************************************************/

	public void setAccelerationToggle(boolean on) throws RetryException {
	}

	/***************************************************************************
	 * pause function
	 * @throws RetryException 
	 **************************************************************************/
	public void delay(long millis) throws RetryException {
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
	 * @throws RetryException 
	 **************************************************************************/
	public void enableDrives() throws RetryException {
		machine.enableDrives();
	}

	public void disableDrives() throws RetryException {
		machine.disableDrives();
	}

	public void enableAxes(EnumSet<AxisId> axes) throws RetryException {
		// Not all drivers support this method.
	}
	
	public void disableAxes(EnumSet<AxisId> axes) throws RetryException {
		// Not all drivers support this method.
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
		this.setMotorDirection(machine.currentTool().getIndex());
	}

	@Override
	public void setMotorDirection(int dir, int toolhead) {
		/// toolhead -1 indicate auto-detect. Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).setMotorDirection(dir);		
	}


	@Override
	public void setMotorRPM(double rpm, int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect. Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).setMotorSpeedRPM(rpm);

	}
	
	@Override
	public void setMotorSpeedPWM(int pwm) throws RetryException {
		this.setMotorSpeedPWM(pwm, machine.currentTool().getIndex());
	}
	
	public void setMotorSpeedPWM(int pwm, int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).setMotorSpeedPWM(pwm);
	}

	
	@Override
	public void enableMotor() throws RetryException {
		this.enableMotor(machine.currentTool().getIndex());
	}

	@Override
	public void enableMotor(long millis) throws RetryException {
		this.enableMotor(millis, machine.currentTool().getIndex());
	}

	@Override
	public void enableMotor(long millis, int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		enableMotor(toolhead);
		delay( millis );
		disableMotor(toolhead);
	}
	@Override
	public void enableMotor(int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).enableMotor();
		
	}


	
	public void disableMotor(int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).disableMotor();
	}

	@Override
	public void disableMotor() throws RetryException {
		this.disableMotor(-1);
		
	}

	public double getMotorRPM() {
		return machine.currentTool().getMotorSpeedRPM();
	}

	public int getMotorSpeedPWM() {
		return machine.currentTool().getMotorSpeedReadingPWM();
	}

	public double getMotorSteps() {
		return machine.currentTool().getMotorSteps();
	}

	// TODO: These are backwards?
	public void readToolStatus() {
	}

	public int getToolStatus() {
		readToolStatus();

		return machine.currentTool().getToolStatus();
	}

	
	/***************************************************************************
	 * Spindle interface functions
	 **************************************************************************/
	public void setSpindleDirection(int dir) {
		machine.currentTool().setSpindleDirection(dir);
	}

	public void setSpindleRPM(double rpm) throws RetryException {
		setSpindleRPM(rpm, -1);
	}

	public void setSpindleSpeedPWM(int pwm) throws RetryException {
		setSpindleSpeedPWM(pwm, -1);
	}

	public void enableSpindle() throws RetryException {
		enableSpindle(-1);
	}

	public void disableSpindle() throws RetryException {
		disableSpindle(-1);
	}
	public void setSpindleRPM(double rpm, int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).setSpindleSpeedRPM(rpm);
	}

	public void setSpindleSpeedPWM(int pwm, int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).setSpindleSpeedPWM(pwm);
	}

	public void enableSpindle(int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).enableSpindle();
	}

	public void disableSpindle(int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).disableSpindle();
	}

	public double getSpindleRPM() {
		return machine.currentTool().getSpindleSpeedReadingRPM();
	}

	public int getSpindleSpeedPWM() {
		return machine.currentTool().getSpindleSpeedReadingPWM();
	}
	
	/***************************************************************************
	 * Temperature interface functions
	 * @throws RetryException 
	 **************************************************************************/
	@Override
	public void setTemperature(double temperature) throws RetryException {
		machine.currentTool().setTargetTemperature(temperature);
	}

	@Override
	public void setTemperature(double temperature, int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).setTargetTemperature(temperature);
	}

	public void readTemperature() {

	}
	public void readTemperature(int toolhead) {

	}

	/** relies on timing to have the 'correct selected toolhead', deprecated */
	@Override
	@Deprecated
	public double getTemperature() {
		return getTemperature(-1); 
	}
	
	@Override
	public double getTemperature(int toolhead) {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		return machine.getTool(toolhead).getCurrentTemperature();
	}

	/***************************************************************************
	 * Platform Temperature interface functions
	 * @throws RetryException 
	 **************************************************************************/
	@Override
	public void setPlatformTemperature(double temperature) throws RetryException {
		this.setPlatformTemperature(temperature, -1);
	}
	
	@Override
	public void setPlatformTemperature(double temperature, int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		machine.getTool(toolhead).setPlatformTargetTemperature(temperature);
	}

	/** relies on timing to have the 'right selected toolhead', deprecated */
	@Deprecated
	public void readPlatformTemperature() {
		this.readPlatformTemperature(-1);
	}

	public void readPlatformTemperature(int toolhead) {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();
	
	}

	/** relies on timing to have the 'right selected toolhead', deprecated */
	@Override 
	@Deprecated
	public double getPlatformTemperature() {
		return this.getPlatformTemperature(-1);
	}

	public double getPlatformTemperature(int toolhead) {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();

		readPlatformTemperature(toolhead);
		return machine.getTool(toolhead).getPlatformCurrentTemperature();
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
	 * @throws RetryException 
	 **************************************************************************/
	public void enableFan() throws RetryException {
		this.enableFan(machine.currentTool().getIndex());		
	}
	@Override
	public void enableFan(int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();
		machine.getTool(toolhead).enableFan();
	}


	public void disableFan() throws RetryException {
		this.disableFan(machine.currentTool().getIndex());
	}

	@Override
	public void disableFan(int toolhead) throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();
		machine.getTool(toolhead).disableFan();
	}
	
	
	public void setAutomatedBuildPlatformRunning(boolean state) throws RetryException {
		this.setAutomatedBuildPlatformRunning(state, machine.currentTool().getIndex());
	}
	@Override
	public void setAutomatedBuildPlatformRunning(boolean state, int toolhead)
			throws RetryException {
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();
		machine.getTool(toolhead).setAutomatedBuildPlatformRunning(state);
	}

	
	public boolean hasAutomatedBuildPlatform()
	{
		return hasAutomatedBuildPlatform(-1);
	}

	public boolean hasAutomatedBuildPlatform(int toolhead)
	{
		/// toolhead -1 indicate auto-detect.Fast hack to get software out..
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();
		return machine.getTool(toolhead).hasAutomatedPlatform();
	}

	/***************************************************************************
	 * Valve interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void openValve() throws RetryException {
		openValve(-1);
	}

	public void closeValve() throws RetryException {
		closeValve(-1);
	}
	public void openValve(int toolhead) throws RetryException {
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();
		machine.getTool(toolhead).openValve();
	}

	public void closeValve(int toolhead) throws RetryException {
		if(toolhead == -1 ) toolhead = machine.currentTool().getIndex();
		machine.getTool(toolhead).closeValve();
	}

	public void setStepperVoltage(int stepperId, int referenceValue) throws RetryException
	{
		Base.logger.fine("BaseImplementation setStepperVoltage called.");
	}
	
//	public void storeStepperVoltage(int stepperId, int referenceValue) throws RetryException
//	{
//		Base.logger.fine("BaseImplementation setStepperVoltage called.");
//	}
	
	public int getStepperVoltage(int stepperId)
	{
		Base.logger.fine("BaseImplementation getStepperVoltage called.");
		return -1;
	}

	
	public void setLedStrip(Color color, int effectId) throws RetryException 
	{
		Base.logger.fine("BaseImplementation setLedStrip called.");
	}
	
	
	public void sendBeep(int frequencyHz, int durationMs,int effect) throws RetryException
	{
		Base.logger.fine("BaseImplementation sendBeep called.");
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
	public void stop(boolean abort) {
		// No implementation needed for synchronous machines.
		Base.logger.info("Machine stop called.");
	}

	public void reset() {
		// No implementation needed for synchronous machines.
		Base.logger.info("Machine reset called.");
	}

	public String getDriverName() {
		return null;
	}
	
	public boolean heartbeat() {
		return true;
	}
	
	public double getChamberTemperature() {
		return 0;
	}

	public void readChamberTemperature() {
	}

	public void setChamberTemperature(double temperature) {
	}

	public double getPlatformTemperatureSetting() {
		return machine.currentTool().getPlatformTargetTemperature();
	}

	public double getTemperatureSetting() {
		return machine.currentTool().getTargetTemperature();
	}

	public void storeHomePositions(EnumSet<AxisId> axes) throws RetryException {
	}

	public void recallHomePositions(EnumSet<AxisId> axes) throws RetryException {
	}

	public boolean hasSoftStop() {

		return hasSoftStop;
	}

	public boolean hasEmergencyStop() {
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
	
	@Override
	//// get machine or situation specific configs
	public String getConfigValue(String value, String baseline) {
		return baseline;
	}


}
