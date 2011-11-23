/*
 SerialPassthroughDriver.java

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

package replicatorg.drivers;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Queue;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;

public class SerialPassthroughDriver extends SerialDriver {
	/**
	 * To keep track of outstanding commands
	 */
	private Queue<Integer> commands;

	/**
	 * the size of the buffer on the GCode host
	 */
	private int maxBufferSize = 128;

	/**
	 * the amount of data we've sent and is in the buffer.
	 */
	private int bufferSize = 0;

	/**
	 * What did we get back from serial?
	 */
	private String result = "";

    private NumberFormat df = Base.getLocalFormat();
    {
    	df.setMaximumFractionDigits(6);
    }
    
	private byte[] responsebuffer = new byte[512];

	public SerialPassthroughDriver() {
		super();

		// init our variables.
		commands = new LinkedList<Integer>();
		bufferSize = 0;
		setInitialized(false);
	}

	public void loadXML(Node xml) {
		super.loadXML(xml);
	}

	public void initialize() {
		// declare our serial guy.
		if (serial == null) {
			Base.logger.severe("No Serial Port found.\n");
			return;
		}
		// wait till we're initialized
		if (!isInitialized()) {
			try {
				// record our start time.
				Date date = new Date();
				long end = date.getTime() + 10000;

				Base.logger.info("Initializing Serial.");
				while (!isInitialized()) {
					readResponse();

					// record our current time
					date = new Date();
					long now = date.getTime();

					// only give them 10 seconds
					if (now > end) {
						Base.logger.warning("Serial link non-responsive.");
						return;
					}
				}
			} catch (Exception e) {
				// todo: handle init exceptions here
			}
			Base.logger.info("Ready.");
		}

		// default us to absolute positioning
		sendCommand("G90");
	}

	public boolean isPassthroughDriver() {
		return true;
	}
	
	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void executeGCodeLine(String code) {
		// we *DONT* want to use the parents one,
		// as that will call all sorts of misc functions.
		// we'll simply pass it along.
		// super.execute();
		sendCommand(code);
	}

	/**
	 * Actually sends command over serial. If the Arduino buffer is full, this
	 * method will block until the command has been sent.
	 */
	protected void sendCommand(String next) {
		assert (isInitialized());
		assert (serial != null);
		// System.out.println("sending: " + next);

		next = clean(next);

		// skip empty commands.
		if (next.length() == 0)
			return;

		// Block until we can fit the command on the Arduino
		while (bufferSize + next.length() + 1 > maxBufferSize) {
			readResponse();
		}

		synchronized (serial) {
			// do the actual send.
			serial.write(next + "\n");
		}

		// record it in our buffer tracker.
		int cmdlen = next.length() + 1;
		commands.add(cmdlen);
		bufferSize += cmdlen;

		// debug... let us know whts up!
		// System.out.println("Sent: " + next);
		// System.out.println("Buffer: " + bufferSize + " (" + bufferLength + "
		// commands)");
	}

	public String clean(String str) {
		String clean = str;

		// trim whitespace
		clean = clean.trim();

		// remove spaces
		clean = clean.replaceAll(" ", "");

		return clean;
	}

	public void readResponse() {
		assert (serial != null);
		synchronized (serial) {
			try {
				int numread = serial.read(responsebuffer);
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
																			// \r
						result = result.substring(index + 1);
						if (line.length() == 0)
							continue;
						if (line.startsWith("ok")) {
							bufferSize -= commands.remove();
							Base.logger.info(line);
						} else if (line.startsWith("T:")) {
							String temp = line.substring(2);
							machine.currentTool().setCurrentTemperature(
									Double.parseDouble(temp));
							Base.logger.info(line);
						}
						// old arduino firmware sends "start"
						else if (line.startsWith("start")) {
							// todo: set version
							setInitialized(true);
							Base.logger.info(line);
						} else if (line.startsWith("Extruder Fail")) {
							setError("Extruder failed:  cannot extrude as this rate.");
							Base.logger.severe(line);
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
	}

	public boolean isFinished() {
		return isBufferEmpty();
	}

	/**
	 * Is our buffer empty? If don't have a buffer, its always true.
	 */
	public boolean isBufferEmpty() {
		try {
			readResponse();
		} catch (Exception e) {
		}
		return (bufferSize == 0);
	}

	public void dispose() {
		super.dispose();
		commands = null;
	}

	/***************************************************************************
	 * commands for interfacing with the driver directly
	 * @throws RetryException 
	 **************************************************************************/

	// FIXME: 5D port
	public void queuePoint(Point5d p) throws RetryException {
		// Redundant feedrate send added in Ultimaker merge. TODO: ask Erik, D1plo1d about this. 
		String cmd = "G1 F" + df.format(getCurrentFeedrate());
		
		sendCommand(cmd);

		cmd = "G1 X" + df.format(p.x()) + " Y" + df.format(p.y()) + " Z"
				+ df.format(p.z()) + " F" + df.format(getCurrentFeedrate());

		sendCommand(cmd);

		super.queuePoint(p);
	}

	// FIXME: 5D port
	public void setCurrentPosition(Point5d p) throws RetryException {
		sendCommand("G92 X" + df.format(p.x()) + " Y" + df.format(p.y()) + " Z"
				+ df.format(p.z()));

		super.setCurrentPosition(p);
	}

	public void homeAxes(EnumSet<AxisId> axes) throws RetryException {
		StringBuffer buf = new StringBuffer("G28 ");
		if (axes.contains(AxisId.X)) buf.append("X");
		if (axes.contains(AxisId.Y)) buf.append("Y");
		if (axes.contains(AxisId.Z)) buf.append("Z");
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

	private String _getToolCode() {
		return "T" + machine.currentTool().getIndex() + " ";
	}

	/***************************************************************************
	 * Motor interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setMotorRPM(double rpm) throws RetryException {
		sendCommand(_getToolCode() + "M108 R" + df.format(rpm));

		super.setMotorRPM(rpm);
	}

	public void setMotorSpeedPWM(int pwm) throws RetryException {
		sendCommand(_getToolCode() + "M108 S" + df.format(pwm));

		super.setMotorSpeedPWM(pwm);
	}

	public void enableMotor() throws RetryException {
		String command = _getToolCode();

		if (machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE)
			command += "M101";
		else
			command += "M102";

		sendCommand(command);

		super.enableMotor();
	}

	public void disableMotor() throws RetryException {
		sendCommand(_getToolCode() + "M103");

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

	public void reset() {
		Base.logger.info("Reset.");
		setInitialized(false);
		initialize();
	}

	protected Point5d reconcilePosition() {
		return new Point5d();
	}
}
