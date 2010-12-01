/*
 RepRap5DDriver.java

 This is a driver to control a machine that contains a GCode parser and communicates via Serial Port.

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
/* TODOs:
 * (M6 T0 (Wait for tool to heat up)) - not supported? Rewrite to other code?
 * 
 * 
 */
package replicatorg.drivers.reprap;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.Point3d;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SerialDriver;
import replicatorg.drivers.reprap.ExtrusionUpdater.Direction;
import replicatorg.machine.model.Axis;
import replicatorg.machine.model.ToolModel;

public class RepRap5DDriver extends SerialDriver {
	private static Pattern gcodeCommentPattern = Pattern.compile("\\([^)]*\\)|;.*");
	private static Pattern resendLinePattern = Pattern.compile("([0-9]+)");
	private static Pattern gcodeLineNumberPattern = Pattern.compile("N\\s*([0-9]+)");
	
	public final AtomicReference<Double> feedrate = new AtomicReference<Double>(0.0);
	public final AtomicReference<Double> ePosition = new AtomicReference<Double>(0.0);
	
	private final ReentrantLock sendCommandLock = new ReentrantLock();
	
	/** true if a line containing the start keyword has been received from the firmware*/
	private final AtomicBoolean startReceived = new AtomicBoolean(false);
	
	private final ResponseReader responseReader = new ResponseReader(this);
	
	/**
	 * Enables five D GCodes if true. If false reverts to traditional 3D Gcodes
	 */
	private boolean fiveD = true;
	
	private final ExtrusionUpdater extrusionUpdater = new ExtrusionUpdater(this);

	/**
	 * To keep track of outstanding commands
	 */
	protected Queue<Integer> commands;

	/**
	 * the size of the buffer on the GCode host
	 */
	private int maxBufferSize = 128;

	/**
	 * the amount of data we've sent and is in the buffer.
	 */
	private int bufferSize = 0;
	
	/**
	 * The commands sent but not yet acknowledged by the firmware. Stored so they can be resent 
	 * if there is a checksum problem.
	 */
	private LinkedList<String> buffer = new LinkedList<String>();
	private ReentrantLock bufferLock = new ReentrantLock();

	/**
	 * What did we get back from serial?
	 */
	private String result = "";

	protected DecimalFormat df;

	private byte[] responsebuffer = new byte[512];

	private AtomicInteger lineNumber = new AtomicInteger(0);

	public RepRap5DDriver() {
		super();
		
		//start the response reader thread
		this.responseReader.start();

		// init our variables.
		commands = new LinkedList<Integer>();
		bufferSize = 0;
		setInitialized(false);

		df = new DecimalFormat("#.######");
	}

	public void loadXML(Node xml) {
		super.loadXML(xml);
	}

	public void updateManualControl() throws InterruptedException
	{
		extrusionUpdater.update();
	}


	public void initialize() {
		// declare our serial guy.
		if (serial == null) {
			Base.logger.severe("No Serial Port found.\n");
			return;
		}
		// wait till we're initialised
		if (!isInitialized()) {
				Base.logger.info("Initializing Serial.");
				//attempt to reset the device, this may slow down the connection time, but it 
				//increases our chances of successfully connecting dramatically.
				Base.logger.fine("Attempting to reset RepRap (pulsing RTS)");
				serial.pulseRTSLow();
				//Wait for the RepRap to startup
				while(startReceived.get()!=true)
				{
					readResponse();
				}
				Base.logger.fine("RepRap reset");

				//Send a line # reset command, this allows us to catch the "ok" response in 
				//case we missed the "start" response which we seem to often miss. (also it 
				//resets the line number which is good)
				sendCommand("M110");


				// record our start time.
				Date date = new Date();
				long end = date.getTime() + 1000;

//				serial.clear();
				Base.logger.fine("first gcode sent. waiting for response..");
				while (!isInitialized()) {
//					readResponse();

/// Recover:
//					Base.logger.warning("No connection; trying to pulse RTS to reset device.");
//					serial.pulseRTSLow();


					// record our current time
					date = new Date();
					long now = date.getTime();

					// only give them 10 seconds
					if (now > end) {
						Base.logger.warning("Serial link non-responsive.");
						return;
					}
				}
				Base.logger.fine("first gcode response received.");
		}

		// default us to absolute positioning
		sendCommand("G90");
		Base.logger.info("Ready.");
	}

	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void execute() {
		// we *DONT* want to use the parents one,
		// as that will call all sorts of misc functions.
		// we'll simply pass it along.
		// super.execute();

		sendCommand(getParser().getCommand());
	}
	
	/**
	 * Returns null or the first instance of the matching group
	 */
	private String getRegexMatch(String regex, String input, int group)
	{
		Matcher matcher = Pattern.compile(regex).matcher(input);
		if (!matcher.find()) return null;
		return matcher.group(1);
	}

	/**
	 * Actually sends command over serial.
	 * 
	 * Commands sent here are acknowledged asynchronously in another 
	 * thread so as to increase our serial GCode throughput.
	 * 
	 * Only one command can be sent at a time. If another command is 
	 * being sent this method will block until the previous command 
	 * is finished sending.
	 */
	protected void sendCommand(String next) {
		sendCommandLock.lock();
		//assert (isInitialized());
		// System.out.println("sending: " + next);

		next = clean(next);
		next = fix(next); // make it compatible with older versions of the GCode interpeter
		
		// skip empty commands.
		if (next.length() == 0)
		{
			return;
		}

		//update the current feedrate
		String feedrate = getRegexMatch("F(-[0-9\\.]+)", next, 1);
		if (feedrate!=null) this.feedrate.set(Double.parseDouble(feedrate));

		//update the current extruder position
		String e = getRegexMatch("E([-0-9\\.]+)", next, 1);
		if (e!=null) this.ePosition.set(Double.parseDouble(e));

		
		next = applyChecksum(next);
		
		// Block until we can fit the command on the Arduino
		while (bufferSize + next.length() + 1 > maxBufferSize) {
			readResponse();
		}

		// do the actual send.
		serialWriteLock.lock();
		assert (serial != null);
		serial.write(next + "\n");
		serialWriteLock.unlock();

		// record it in our buffer tracker.
		int cmdlen = next.length() + 1;
		bufferLock.lock();
		commands.add(cmdlen);
		bufferSize += cmdlen;
		buffer.addFirst(next);
		bufferLock.unlock();

		// debug... let us know whats up!
		Base.logger.fine("Sent: " + next);

		// Wait for the response (synchronous gcode transmission)
		//while(!isFinished()) {}
		
		// Notify the response reader that another command has been sent so it should 
		// await a response.
		responseReader.notifyCommandSent();

		sendCommandLock.unlock();
	}

	public String clean(String str) {
		String clean = str;

		// trim whitespace
		clean = clean.trim();

		// remove spaces
		//clean = clean.replaceAll(" ", "");
		
		// remove all comments
        clean = RepRap5DDriver.gcodeCommentPattern.matcher(clean).replaceAll("");

		return clean;
	}
	public String fix(String str) {
		String fixed = str;
		// The 5D firmware expects E codes for extrusion control instead of M101, M102, M103

		Pattern r = Pattern.compile("M01[^0-9]");
		Matcher m = r.matcher(fixed);
		if (m.find())
		{
			return "";
		}

	    // Remove M10[123] codes
	    // This piece of code causes problems?!? Restarts?
		r = Pattern.compile("M10[123](.*)");
		m = r.matcher(fixed);
	    if (m.find( )) {
//	    	System.out.println("Didn't find pattern in: " + str );
//	    	fixed = m.group(1)+m.group(3)+";";
	    	return "";
	    }
		
		// Reorder E and F codes? F-codes need to go LAST!
		//requires: import java.util.regex.Matcher; and import java.util.regex.Pattern;
		r = Pattern.compile("^(.*)(F[0-9\\.]*)\\s?E([0-9\\.]*)$");
	    m = r.matcher(fixed);
	    if (m.find( )) {
			fixed = m.group(1)+" E"+m.group(3)+" "+m.group(2);
	    }

	    // Rescale E value
//		r = Pattern.compile("(.*)E([0-9\\.]*)(.*)");//E317.52// Z-moves slower! Extrude 10% less? More and more rapid reversing
//	    m = r.matcher(fixed);
//	    if (m.find( )) {
//	    	double newEvalue = Double.valueOf(m.group(2).trim()).doubleValue();
//	    	newEvalue = newEvalue * 0.040;
//	    	NumberFormat formatter = new DecimalFormat("#0.0");
//	    	fixed = m.group(1)+" E"+formatter.format(newEvalue)+" "+m.group(3);
//	    }

	    
	    return fixed; // no change!
	}

	/**
	 * takes a line of gcode and returns that gcode with a line number and checksum
	 */
	public String applyChecksum(String gcode) {
		// RepRap Syntax: N<linenumber> <cmd> *<chksum>\n

		if (gcode.contains("M110"))
			lineNumber.set(0);
		
		Matcher lineNumberMatcher = gcodeLineNumberPattern.matcher(gcode);
		if (lineNumberMatcher.matches())
		{ // reset our line number to the specified one. this is usually a m110 line # reset
			lineNumber.set( Integer.parseInt( lineNumberMatcher.group(1) ) );
		}
		else
		{ // only add a line number if it is not already specified
			gcode = "N"+lineNumber.incrementAndGet()+' '+gcode+' ';
		}

		// chksum = 0 xor each byte of the gcode (including the line number and trailing space)
		byte checksum = 0;
		byte[] gcodeBytes = gcode.getBytes();
		for (int i = 0; i<gcodeBytes.length; i++)
		{
			checksum = (byte) (checksum ^ gcodeBytes[i]);
		}
		return gcode+'*'+checksum;
	}
	
	public void readResponse() {
		try {
			int numread;

			serialWriteLock.lock();
			assert (serial != null);
			synchronized (serial) {
				numread = serial.read(responsebuffer);
			}
			serialWriteLock.unlock();

			// 0 is now an acceptable value; it merely means that we timed out
			// waiting for input
			if (numread < 0) {
				// This signifies EOF. FIXME: How do we handle this?
				Base.logger.severe("SerialPassthroughDriver.readResponse(): EOF occured");
				return;
			} else {
				result += new String(responsebuffer, 0, numread, "US-ASCII");

				// System.out.println("got: " + c);
				// System.out.println("current: " + result);
				int index;
				while ((index = result.indexOf('\n')) >= 0) {
					String line = result.substring(0, index).trim(); // trim
																		// to
																		// remove
																		// any
																		// trailing
					Base.logger.fine(line);											// \r
					result = result.substring(index + 1);
					if (line.length() == 0)
						continue;
					if (line.startsWith("ok")) {

						setInitialized(true);
						bufferLock.lock();
						bufferSize -= commands.remove();
						buffer.removeLast();
						bufferLock.unlock();

						if (line.startsWith("ok T:")) {
							Pattern r = Pattern.compile("^ok T:([0-9\\.]+)");
						    Matcher m = r.matcher(line);
						    if (m.find( )) {
						    	String temp = m.group(1);
								
								machine.currentTool().setCurrentTemperature(
										Double.parseDouble(temp));
						    }
							r = Pattern.compile("^ok.*B:([0-9\\.]+)$");
						    m = r.matcher(line);
						    if (m.find( )) {
						    	String bedTemp = m.group(1);
								machine.currentTool().setPlatformCurrentTemperature(
										Double.parseDouble(bedTemp));
						    }
						}

					}
					// old arduino firmware sends "start"
					else if (line.contains("start")||line.contains("Start")) {
						// todo: set version
						// TODO: check if this was supposed to happen, otherwise report unexpected reset! 
						startReceived.set(true);
						lineNumber.set(0);

					} else if (line.startsWith("Extruder Fail")) {
						setError("Extruder failed:  cannot extrude as this rate.");

					} else if (line.startsWith("Resend:")||line.startsWith("rs ")) {
						Base.logger.severe(line);
						// Bad checksum, resend requested
						bufferLock.lock();
						String bufferedLine = buffer.removeLast();
						bufferSize -= commands.remove();
						bufferLock.unlock();

						int bufferedLineNumber = Integer.parseInt( 
								gcodeLineNumberPattern.matcher(bufferedLine).group(1) );

						Matcher badLineMatch = resendLinePattern.matcher(line);
						if (badLineMatch.find())
						{
							int badLineNumber = Integer.parseInt(
									badLineMatch.group(1) );

							if (bufferedLineNumber != badLineNumber)
							{
								Base.logger.warning("unexpected line number, resetting line number");
								// reset the line number if it does not match the buffered line
								 this.sendCommand("N"+(bufferedLineNumber-1)+" M110");
							}
						}
						else
						{
							// Malformed resend line request received. Resetting the line number
							Base.logger.warning("malformed line resend request, "
									+"resetting line number. Malformed Data: \n"+line);
							this.sendCommand("N"+(bufferedLineNumber-1)+" M110");
						}

						// resend the line
						this.sendCommand(bufferedLine);
					} else {
						Base.logger.severe("Unknown: " + line);
					}
				}
			}
		} catch (IOException e) {
			Base.logger.severe("inputstream.read() failed: " + e.toString());
			// FIXME: Shut down communication somehow.
		}
	}

	public boolean isFinished() {
		return isBufferEmpty();
	}

	/**
	 * Is our buffer empty? If don't have a buffer, its always true.
	 */
	public boolean isBufferEmpty() {
//		try {
//			readResponse();
//		} catch (Exception e) {
//		}
		bufferLock.lock();
		boolean isEmpty = (bufferSize == 0);
		bufferLock.unlock();
		return isEmpty;
	}

	public void dispose() {
		this.responseReader.dispose();
		super.dispose();

		if (serial != null)
			serial.dispose();
		serial = null;
		commands = null;
	}

	/***************************************************************************
	 * commands for interfacing with the driver directly
	 * @throws RetryException 
	 **************************************************************************/

	public void queuePoint(Point3d p) throws RetryException {
		String cmd = "G1 F" + df.format(getCurrentFeedrate());
		
		sendCommand(cmd);

		cmd = "G1 X" + df.format(p.x) + " Y" + df.format(p.y) + " Z"
				+ df.format(p.z) + " F" + df.format(getCurrentFeedrate());

		sendCommand(cmd);

		super.queuePoint(p);
	}

	public void setCurrentPosition(Point3d p) throws RetryException {
		sendCommand("G92 X" + df.format(p.x) + " Y" + df.format(p.y) + " Z"
				+ df.format(p.z));

		super.setCurrentPosition(p);
	}

	@Override
	public void homeAxes(EnumSet<Axis> axes, boolean positive, double feedrate) throws RetryException {
		Base.logger.info("homing "+axes.toString());
		StringBuffer buf = new StringBuffer("G28");
		for (Axis axis : axes)
		{
			buf.append(" "+axis);
		}
		sendCommand(buf.toString());

		super.homeAxes(axes,false,0);
	}

	public void delay(long millis) {
		int seconds = Math.round(millis / 1000);

		sendCommand("G4 P" + seconds);

		// no super call requried.
	}

	public void openClamp(int clampIndex) {
		sendCommand("M11 Q" + clampIndex);

		super.openClamp(clampIndex);
	}

	public void closeClamp(int clampIndex) {
		sendCommand("M10 Q" + clampIndex);

		super.closeClamp(clampIndex);
	}

	public void enableDrives() throws RetryException {
		sendCommand("M17");

		super.enableDrives();
	}

	public void disableDrives() throws RetryException {
		sendCommand("M18");

		super.disableDrives();
	}

	public void changeGearRatio(int ratioIndex) {
		// gear ratio codes are M40-M46
		int code = 40 + ratioIndex;
		code = Math.max(40, code);
		code = Math.min(46, code);

		sendCommand("M" + code);

		super.changeGearRatio(ratioIndex);
	}

	protected String _getToolCode() {
		return "T" + machine.currentTool().getIndex() + " ";
	}

	/***************************************************************************
	 * Motor interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setMotorRPM(double rpm) throws RetryException {
		if (fiveD == false)
		{
			sendCommand(_getToolCode() + "M108 R" + df.format(rpm));
		}
		else
		{
			extrusionUpdater.setFeedrate(rpm);
		}
		
		super.setMotorRPM(rpm);
	}

	public void setMotorSpeedPWM(int pwm) throws RetryException {
		if (fiveD == false)
		{
			sendCommand(_getToolCode() + "M108 S" + df.format(pwm));
		}

		super.setMotorSpeedPWM(pwm);
	}
	
	public synchronized void enableMotor() throws RetryException {
		String command = _getToolCode();

		if (fiveD == false)
		{
			if (machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE)
				command += "M101";
			else
				command += "M102";
	
			sendCommand(command);
		}
		else
		{
			extrusionUpdater.setDirection( machine.currentTool().getMotorDirection()==1?
					Direction.forward : Direction.reverse );
			extrusionUpdater.startExtruding();
		}

		super.enableMotor();
	}

	public void disableMotor() throws RetryException {
		if (fiveD == false)
		{
			sendCommand(_getToolCode() + "M103");
		}
		else
		{
			extrusionUpdater.stopExtruding();
		}

		super.disableMotor();
	}

	/***************************************************************************
	 * Spindle interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setSpindleRPM(double rpm) throws RetryException {
		sendCommand(_getToolCode() + "S" + df.format(rpm));

		super.setSpindleRPM(rpm);
	}

	public void enableSpindle() throws RetryException {
		String command = _getToolCode();

		if (machine.currentTool().getSpindleDirection() == ToolModel.MOTOR_CLOCKWISE)
			command += "M3";
		else
			command += "M4";

		sendCommand(command);

		super.enableSpindle();
	}

	public void disableSpindle() throws RetryException {
		sendCommand(_getToolCode() + "M5");

		super.disableSpindle();
	}

	/***************************************************************************
	 * Temperature interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setTemperature(double temperature) throws RetryException {
		sendCommand(_getToolCode() + "M104 S" + df.format(temperature));

		super.setTemperature(temperature);
	}

	public void readTemperature() {
		sendCommand(_getToolCode() + "M105");

		super.readTemperature();
	}

	public double getPlatformTemperature(){
		return machine.currentTool().getPlatformCurrentTemperature();
	}
	/***************************************************************************
	 * Flood Coolant interface functions
	 **************************************************************************/
	public void enableFloodCoolant() {
		sendCommand(_getToolCode() + "M7");

		super.enableFloodCoolant();
	}

	public void disableFloodCoolant() {
		sendCommand(_getToolCode() + "M9");

		super.disableFloodCoolant();
	}

	/***************************************************************************
	 * Mist Coolant interface functions
	 **************************************************************************/
	public void enableMistCoolant() {
		sendCommand(_getToolCode() + "M8");

		super.enableMistCoolant();
	}

	public void disableMistCoolant() {
		sendCommand(_getToolCode() + "M9");

		super.disableMistCoolant();
	}

	/***************************************************************************
	 * Fan interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void enableFan() throws RetryException {
		sendCommand(_getToolCode() + "M106");

		super.enableFan();
	}

	public void disableFan() throws RetryException {
		sendCommand(_getToolCode() + "M107");

		super.disableFan();
	}

	/***************************************************************************
	 * Valve interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void openValve() throws RetryException {
		sendCommand(_getToolCode() + "M126");

		super.openValve();
	}

	public void closeValve() throws RetryException {
		sendCommand(_getToolCode() + "M127");

		super.closeValve();
	}

	/***************************************************************************
	 * Collet interface functions
	 **************************************************************************/
	public void openCollet() {
		sendCommand(_getToolCode() + "M21");

		super.openCollet();
	}

	public void closeCollet() {
		sendCommand(_getToolCode() + "M22");

		super.closeCollet();
	}

	public synchronized void reset() {
		Base.logger.info("Reset.");
		setInitialized(false);
		// resetting the serial port + command queue
		//this.serial.clear();
		//commands = null;

		initialize();
	}

	protected Point3d reconcilePosition() {
		return new Point3d(0,0,0);
	}
}

