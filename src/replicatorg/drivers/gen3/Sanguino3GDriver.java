/*
 Sanguino3GDriver.java

 This is a driver to control a machine that uses the Sanguino with 3rd Generation Electronics.

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

package replicatorg.drivers.gen3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import javax.vecmath.Point3d;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.TimeoutException;
import replicatorg.drivers.BadFirmwareVersionException;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.SDCardCapture;
import replicatorg.drivers.SerialDriver;
import replicatorg.drivers.Version;
import replicatorg.machine.model.Axis;
import replicatorg.machine.model.ToolModel;
import replicatorg.uploader.FirmwareUploader;

public class Sanguino3GDriver extends SerialDriver
	implements OnboardParameters, SDCardCapture
{
	/**
	 * An enumeration of the available command codes for the three-axis CNC
	 * stage.
	 */
	enum CommandCodeMaster {
		VERSION(0),
		INIT(1),
		GET_BUFFER_SIZE(2),
		CLEAR_BUFFER(3),
		GET_POSITION(4),
		GET_RANGE(5),
		SET_RANGE(6),
		ABORT(7),
		PAUSE(8),
		PROBE(9),
		TOOL_QUERY(10),
		IS_FINISHED(11),
		READ_EEPROM(12),
		WRITE_EEPROM(13),
		
		CAPTURE_TO_FILE(14),
		END_CAPTURE(15),
		PLAYBACK_CAPTURE(16),
		
		RESET(17),

		NEXT_FILENAME(18),
		
		// QUEUE_POINT_INC(128) obsolete
		QUEUE_POINT_ABS(129),
		SET_POSITION(130),
		FIND_AXES_MINIMUM(131),
		FIND_AXES_MAXIMUM(132),
		DELAY(133),
		CHANGE_TOOL(134),
		WAIT_FOR_TOOL(135),
		TOOL_COMMAND(136),
		ENABLE_AXES(137);
		
		private int code;
		private CommandCodeMaster(int code) {
			this.code = code;
		}
		int getCode() { return code; }
	};

	/**
	 * An enumeration of the available command codes for a tool.
	 */
	enum CommandCodeSlave {
		VERSION(0),
		INIT(1),
		GET_TEMP(2),
		SET_TEMP(3),
		SET_MOTOR_1_PWM(4),
		SET_MOTOR_2_PWM(5),
		SET_MOTOR_1_RPM(6),
		SET_MOTOR_2_RPM(7),
		SET_MOTOR_1_DIR(8),
		SET_MOTOR_2_DIR(9),
		TOGGLE_MOTOR_1(10),
		TOGGLE_MOTOR_2(11),
		TOGGLE_FAN(12),
		TOGGLE_VALVE(13),
		SET_SERVO_1_POS(14),
		SET_SERVO_2_POS(15),
		FILAMENT_STATUS(16),
		GET_MOTOR_1_RPM(17),
		GET_MOTOR_2_RPM(18),
		GET_MOTOR_1_PWM(19),
		GET_MOTOR_2_PWM(20),
		SELECT_TOOL(21),
		IS_TOOL_READY(22),
		READ_FROM_EEPROM(25),
		WRITE_TO_EEPROM(26),
		GET_PLATFORM_TEMP(30),
		SET_PLATFORM_TEMP(31);
		
		private int code;
		private CommandCodeSlave(int code) {
			this.code = code;
		}
		int getCode() { return code; }
	};

	public Sanguino3GDriver() {
		super();

		// This driver only covers v1.X firmware
		minimumVersion = new Version(1,1);
		preferredVersion = new Version(1,3);
		// init our variables.
		setInitialized(false);
	}

	public void loadXML(Node xml) {
		super.loadXML(xml);

	}

	public void initialize() {
		// Create our serial object
		if (serial == null) {
			System.out.println("No serial port found.\n");
			return;
		}

		// wait till we're initialized
		if (!isInitialized()) {
			// attempt to send version command and retrieve reply.
			try {
				waitForStartup(5000);
			} catch (Exception e) {
				// todo: handle init exceptions here
				e.printStackTrace();
			}
		}

		// did it actually work?
		if (isInitialized()) {
			// okay, take care of version info /etc.
			if (version.compareTo(getMinimumVersion()) < 0) {
				throw new BadFirmwareVersionException(version,getMinimumVersion());
			}
			sendInit();
			super.initialize();

			System.out.println("Ready to print.");

			return;
		} else {
			System.out.println("Unable to connect to firmware.");
		}
	}

	/**
	 * Wait for a startup message. After the specified timeout, replicatorG will
	 * attempt to remotely reset the device.
	 * 
	 * @timeoutMillis the time, in milliseconds, that we should wait for a
	 *                handshake.
	 * @return true if we recieved a handshake; false if we timed out.
	 */
	protected void waitForStartup(int timeoutMillis) {
		assert (serial != null);
		//System.err.println("Wait for startup");
		synchronized (serial) {
			serial.setTimeout(timeoutMillis);
			waitForStartupMessage();
			try {
				version = getVersionInternal();
				if (getVersion() != null)
					setInitialized(true);
			} catch (TimeoutException e) {
				// Timed out waiting; try an explicit reset.
				System.out.println("No connection; trying to pulse RTS to reset device.");
				serial.pulseRTSLow();
				waitForStartupMessage();
			}
		}
		// Until we fix the firmware hangs, turn off timeout during
		// builds.
		// TODO: put the timeout back in
		serial.setTimeout(0);
	}

	private void waitForStartupMessage() {
		try {
			Thread.sleep(3000); // wait for startup
		} catch (InterruptedException ie) { 
			serial.setTimeout(0);
			return;
		}
		byte[] response = new byte[256];
		StringBuffer respSB = new StringBuffer();
		try {
			while (serial.available() > 0) {
				serial.read(response);
				respSB.append(response);
			}
			//System.err.println("Received "+ respSB.toString());
		} catch (TimeoutException te) {
		}
	}
	/**
	 * Sends the command over the serial connection and retrieves a result.
	 */
	protected PacketResponse runCommand(byte[] packet) {
		assert (serial != null);
		
		if (packet == null || packet.length < 4)
			return null; // skip empty commands or broken commands

		if (fileCaptureOstream != null) {
			// capture to file.
			try {
				if ((packet[2] & 0x80) != 0) { // ignore query commands
					fileCaptureOstream.write(packet,2,packet.length-3);
				} 
			} catch (IOException ioe) {
				// IOE should be very rare and shouldn't have to contaminate
				// our whole call stack; we'll wrap it in a runtime error.
				throw new RuntimeException(ioe);
			}
			return PacketResponse.okResponse();  // Always pretend that it's all good.
		}
		
		boolean packetSent = false;
		PacketProcessor pp = new PacketProcessor();
		PacketResponse pr = new PacketResponse();

		while (!packetSent) {
			pp = new PacketProcessor();

			synchronized (serial) {
				serial.write(packet);

				if (Base.logger.isLoggable(Level.FINER)) {
					StringBuffer buf = new StringBuffer("OUT: ");
					for (int i = 0; i < packet.length; i++) {
						buf.append(Integer
								.toHexString((int) packet[i] & 0xff));
						buf.append(" ");
					}
					Base.logger.log(Level.FINER,buf.toString());
				}

					boolean c = false;
					while (!c) {
						int b = serial.read();
						if (b == -1) {
							/// Windows has no timeout; busywait
							if (Base.isWindows()) continue;
							throw new TimeoutException(serial);
						}
						c = pp.processByte((byte) b);
					}

					pr = pp.getResponse();

					if (pr.isOK())
						packetSent = true;
					else if (pr.getResponseCode() == PacketResponse.ResponseCode.BUFFER_OVERFLOW) {
						try {
							Thread.sleep(25);
						} catch (Exception e) {
						}
					}
					// TODO: implement other error things.
					else
						break;

			}
		}
		pr.printDebug();
		return pr;
	}

	static boolean isNotifiedFinishedFeature = false;

	public boolean isFinished() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.IS_FINISHED.getCode());
		PacketResponse pr = runCommand(pb.getPacket());
		int v = pr.get8();
		if (pr.getResponseCode() == PacketResponse.ResponseCode.UNSUPPORTED) {
			if (!isNotifiedFinishedFeature) {
				System.out.println("IsFinished not supported; update your firmware.");
				isNotifiedFinishedFeature = true;
			}
			return true;
		}
		boolean finished = (v != 0);
		Base.logger.log(Level.FINE,"Is finished: " + Boolean.toString(finished));
		return finished;
	}

	public void dispose() {
		super.dispose();
	}

	/***************************************************************************
	 * commands used internally to driver
	 **************************************************************************/
	public Version getVersionInternal() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.VERSION.getCode());
		pb.add16(Base.VERSION);

		PacketResponse pr = runCommand(pb.getPacket());
		int versionNum = pr.get16();

		Base.logger.log(Level.FINE,"Reported version: "
					+ Integer.toHexString(versionNum));
		if (versionNum == 0) {
			System.err.println("Null version reported!");
			return null;
		}
		Version v = new Version(versionNum / 100, versionNum % 100);
		System.out.println("Motherboard firmware v"+v);

		final String MB_NAME = "RepRap Motherboard v1.X"; 
		FirmwareUploader.checkLatestVersion(MB_NAME, v);

		PacketBuilder slavepb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		slavepb.add8((byte) machine.currentTool().getIndex());
		slavepb.add8(CommandCodeSlave.VERSION.getCode());
		PacketResponse slavepr = runCommand(slavepb.getPacket());

		int slaveVersionNum = slavepr.get16();
		Base.logger.log(Level.FINE,"Reported slave board version: "
					+ Integer.toHexString(slaveVersionNum));
		if (slaveVersionNum == 0)
			System.err.println("Extruder board: Null version reported!");
        else
        {
            Version sv = new Version(slaveVersionNum / 100, slaveVersionNum % 100);
            System.out.println("Extruder controller firmware v"+sv);

            final String EC_NAME = "Extruder Controller v2.2"; 
    		FirmwareUploader.checkLatestVersion(EC_NAME, sv);
        }
        
		return v;
	}
	
	

	public void sendInit() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.INIT.getCode());
		runCommand(pb.getPacket());
	}

	/***************************************************************************
	 * commands for interfacing with the driver directly
	 **************************************************************************/

	public void queuePoint(Point3d p) {
		Base.logger.log(Level.FINE,"Queued point " + p);

		// is this point even step-worthy?
		Point3d deltaSteps = getAbsDeltaSteps(getCurrentPosition(), p);
		double masterSteps = getLongestLength(deltaSteps);

		// okay, we need at least one step.
		if (masterSteps > 0.0) {
			// where we going?
			Point3d steps = machine.mmToSteps(p);

			// how fast are we doing it?
			long micros = convertFeedrateToMicros(getCurrentPosition(),
					p, getSafeFeedrate(deltaSteps));

			// okay, send it off!
			queueAbsolutePoint(steps, micros);

			super.queuePoint(p);
		}
	}

	public Point3d getPosition() {
		return new Point3d();
	}

	/*
	 * //figure out the axis with the most steps. Point3d steps =
	 * getAbsDeltaSteps(getCurrentPosition(), p); Point3d delta_steps =
	 * getDeltaSteps(getCurrentPosition(), p); int max = Math.max((int)steps.x,
	 * (int)steps.y); max = Math.max(max, (int)steps.z);
	 * 
	 * //get the ratio of steps to take each segment double xRatio = steps.x /
	 * max; double yRatio = steps.y / max; double zRatio = steps.z / max;
	 * 
	 * //how many segments will there be? int segmentCount = (int)Math.ceil(max /
	 * 32767.0);
	 * 
	 * //within our range? just do it. if (segmentCount == 1)
	 * queueIncrementalPoint(pb, delta_steps, ticks); else { for (int i=0; i<segmentCount;
	 * i++) { Point3d segmentSteps = new Point3d();
	 * 
	 * //TODO: is this accurate? //TODO: factor in negative deltas! //calculate
	 * our line segments segmentSteps.x = Math.round(32767 * xRatio);
	 * segmentSteps.y = Math.round(32767 * yRatio); segmentSteps.z =
	 * Math.round(32767 * zRatio);
	 * 
	 * //keep track of them. steps.x -= segmentSteps.x; steps.y -=
	 * segmentSteps.y; steps.z -= segmentSteps.z;
	 * 
	 * //send this segment queueIncrementalPoint(pb, segmentSteps, ticks); } }
	 */

	private void queueAbsolutePoint(Point3d steps, long micros) {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.QUEUE_POINT_ABS.getCode());

		if (Base.logger.isLoggable(Level.FINE)) {
			Base.logger.log(Level.FINE,"Queued absolute point " + steps + " at "
					+ Long.toString(micros) + " usec.");
		}

		// just add them in now.
		pb.add32((int) steps.x);
		pb.add32((int) steps.y);
		pb.add32((int) steps.z);
		pb.add32((int) micros);

		runCommand(pb.getPacket());
	}

	public void setCurrentPosition(Point3d p) {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.SET_POSITION.getCode());

		Point3d steps = machine.mmToSteps(p);
		pb.add32((long) steps.x);
		pb.add32((long) steps.y);
		pb.add32((long) steps.z);

		Base.logger.log(Level.FINE,"Set current position to " + p + " (" + steps
					+ ")");

		runCommand(pb.getPacket());

		super.setCurrentPosition(p);
	}

	public void homeAxes(EnumSet<Axis> axes) {
		Base.logger.log(Level.FINE,"Homing axes "+axes.toString());
		byte flags = 0x00;

		// figure out our fastest feedrate.
		Point3d maxFeedrates = machine.getMaximumFeedrates();
		double feedrate = Math.max(maxFeedrates.x, maxFeedrates.y);
		feedrate = Math.max(maxFeedrates.z, feedrate);

		Point3d target = new Point3d();
		
		if (axes.contains(Axis.X)) {
			flags += 1;
			feedrate = Math.min(feedrate, maxFeedrates.x);
			target.x = 1; // just to give us feedrate info.
		}
		if (axes.contains(Axis.Y)) {
			flags += 2;
			feedrate = Math.min(feedrate, maxFeedrates.y);
			target.y = 1; // just to give us feedrate info.
		}
		if (axes.contains(Axis.Z)) {
			flags += 4;
			feedrate = Math.min(feedrate, maxFeedrates.z);
			target.z = 1; // just to give us feedrate info.
		}
		
		// calculate ticks
		long micros = convertFeedrateToMicros(new Point3d(), target, feedrate);
		// send it!
		PacketBuilder pb = new PacketBuilder(
				CommandCodeMaster.FIND_AXES_MINIMUM.getCode());
		pb.add8(flags);
		pb.add32((int) micros);
		pb.add16(300); // default to 5 minutes
		
		runCommand(pb.getPacket());
	}
		

	public void delay(long millis) {
		if (Base.logger.isLoggable(Level.FINER)) {
			Base.logger.log(Level.FINER,"Delaying " + millis + " millis.");
		}

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.DELAY.getCode());
		pb.add32(millis);
		runCommand(pb.getPacket());
	}

	public void openClamp(int clampIndex) {
		// TODO: throw some sort of unsupported exception.
		super.openClamp(clampIndex);
	}

	public void closeClamp(int clampIndex) {
		// TODO: throw some sort of unsupported exception.
		super.closeClamp(clampIndex);
	}

	public void enableDrives() {
		// Command RMB to enable its steppers. Note that they are
		// already automagically enabled by most commands and need
		// not be explicitly enabled.
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.ENABLE_AXES.getCode());
		pb.add8(0x87); // enable x,y,z
		runCommand(pb.getPacket());
		super.enableDrives();
	}

	public void disableDrives() {
		// Command RMB to disable its steppers.
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.ENABLE_AXES.getCode());
		pb.add8(0x07); // disable x,y,z
		runCommand(pb.getPacket());
		super.disableDrives();
	}

	public void changeGearRatio(int ratioIndex) {
		// TODO: throw some sort of unsupported exception.
		super.changeGearRatio(ratioIndex);
	}

	public void requestToolChange(int toolIndex) {
		selectTool(toolIndex);

		Base.logger.log(Level.FINE,"Waiting for tool #" + toolIndex);

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.WAIT_FOR_TOOL.getCode());
		pb.add8((byte) toolIndex);
		pb.add16(100); // delay between master -> slave pings (millis)
		pb.add16(120); // timeout before continuing (seconds)
		runCommand(pb.getPacket());
	}

	public void selectTool(int toolIndex) {
		Base.logger.log(Level.FINE,"Selecting tool #" + toolIndex);

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.CHANGE_TOOL.getCode());
		pb.add8((byte) toolIndex);
		runCommand(pb.getPacket());

		super.selectTool(toolIndex);
	}

	/***************************************************************************
	 * Motor interface functions
	 **************************************************************************/
	public void setMotorRPM(double rpm) {
		// convert RPM into microseconds and then send.
		long microseconds = (int) Math.round(60.0 * 1000000.0 / rpm); // no
		// unsigned
		// ints?!?
		// microseconds = Math.min(microseconds, 2^32-1); // limit to uint32.

		Base.logger.log(Level.FINE,"Setting motor 1 speed to " + rpm + " RPM ("
					+ microseconds + " microseconds)");

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.SET_MOTOR_1_RPM.getCode());
		pb.add8((byte) 4); // length of payload.
		pb.add32(microseconds);
		runCommand(pb.getPacket());

		super.setMotorRPM(rpm);
	}

	public void setMotorSpeedPWM(int pwm) {
		Base.logger.log(Level.FINE,"Setting motor 1 speed to " + pwm + " PWM");

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.SET_MOTOR_1_PWM.getCode());
		pb.add8((byte) 1); // length of payload.
		pb.add8((byte) pwm);
		runCommand(pb.getPacket());

		super.setMotorSpeedPWM(pwm);
	}

	public void enableMotor() {
		// our flag variable starts with motors enabled.
		byte flags = 1;

		// bit 1 determines direction...
		if (machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE)
			flags += 2;

		Base.logger.log(Level.FINE,"Toggling motor 1 w/ flags: "
					+ Integer.toBinaryString(flags));

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.TOGGLE_MOTOR_1.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8(flags);
		runCommand(pb.getPacket());

		super.enableMotor();
	}

	public void disableMotor() {
		// bit 1 determines direction...
		byte flags = 0;
		if (machine.currentTool().getSpindleDirection() == ToolModel.MOTOR_CLOCKWISE)
			flags += 2;

		Base.logger.log(Level.FINE,"Disabling motor 1");

		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.TOGGLE_MOTOR_1.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8(flags);
		runCommand(pb.getPacket());

		super.disableMotor();
	}

	public int getMotorSpeedPWM() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.GET_MOTOR_1_PWM.getCode());
		PacketResponse pr = runCommand(pb.getPacket());

		// get it
		int pwm = pr.get8();

		Base.logger.log(Level.FINE,"Current motor 1 PWM: " + pwm);

		// set it.
		machine.currentTool().setMotorSpeedReadingPWM(pwm);

		return pwm;
	}

	public double getMotorSpeedRPM() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.GET_MOTOR_1_RPM.getCode());
		PacketResponse pr = runCommand(pb.getPacket());

		// convert back to RPM
		long micros = pr.get32();
		double rpm = (60.0 * 1000000.0 / micros);

		Base.logger.log(Level.FINE,"Current motor 1 RPM: " + rpm + " (" + micros + ")");

		// set it.
		machine.currentTool().setMotorSpeedReadingRPM(rpm);

		return rpm;
	}

	/***************************************************************************
	 * Spindle interface functions
	 **************************************************************************/
	public void setSpindleRPM(double rpm) {
		// convert RPM into microseconds and then send.
		long microseconds = (int) Math.round(60 * 1000000 / rpm); // no
		// unsigned
		// ints?!?
		microseconds = Math.min(microseconds, 2 ^ 32 - 1); // limit to uint32.

		Base.logger.log(Level.FINE,"Setting motor 2 speed to " + rpm + " RPM ("
					+ microseconds + " microseconds)");

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.SET_MOTOR_2_RPM.getCode());
		pb.add8((byte) 4); // payload length
		pb.add32(microseconds);
		runCommand(pb.getPacket());

		super.setSpindleRPM(rpm);
	}

	public void setSpindleSpeedPWM(int pwm) {
		Base.logger.log(Level.FINE,"Setting motor 2 speed to " + pwm + " PWM");

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.SET_MOTOR_2_PWM.getCode());
		pb.add8((byte) 1); // length of payload.
		pb.add8((byte) pwm);
		runCommand(pb.getPacket());

		super.setMotorSpeedPWM(pwm);
	}

	public void enableSpindle() {
		// our flag variable starts with spindles enabled.
		byte flags = 1;

		// bit 1 determines direction...
		if (machine.currentTool().getSpindleDirection() == ToolModel.MOTOR_CLOCKWISE)
			flags += 2;

		Base.logger.log(Level.FINE,"Toggling motor 2 w/ flags: "
					+ Integer.toBinaryString(flags));

		// send it!
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.TOGGLE_MOTOR_2.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8(flags);
		runCommand(pb.getPacket());

		super.enableSpindle();
	}

	public void disableSpindle() {
		// bit 1 determines direction...
		byte flags = 0;
		if (machine.currentTool().getSpindleDirection() == ToolModel.MOTOR_CLOCKWISE)
			flags += 2;

		Base.logger.log(Level.FINE,"Disabling motor 2");

		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.TOGGLE_MOTOR_1.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8(flags);
		runCommand(pb.getPacket());

		super.disableSpindle();
	}

	public double getSpindleSpeedRPM() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.GET_MOTOR_2_RPM.getCode());
		PacketResponse pr = runCommand(pb.getPacket());

		// convert back to RPM
		long micros = pr.get32();
		double rpm = (60.0 * 1000000.0 / micros);

		Base.logger.log(Level.FINE,"Current motor 2 RPM: " + rpm + " (" + micros
					+ ")");

		// set it.
		machine.currentTool().setSpindleSpeedReadingRPM(rpm);

		return rpm;
	}

	public int getSpindleSpeedPWM() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.GET_MOTOR_2_PWM.getCode());
		PacketResponse pr = runCommand(pb.getPacket());

		// get it
		int pwm = pr.get8();

		Base.logger.log(Level.FINE,"Current motor 1 PWM: " + pwm);

		// set it.
		machine.currentTool().setSpindleSpeedReadingPWM(pwm);

		return pwm;
	}

	/***************************************************************************
	 * Temperature interface functions
	 **************************************************************************/
	public void setTemperature(double temperature) {
		// constrain our temperature.
		int temp = (int) Math.round(temperature);
		temp = Math.min(temp, 65535);

		Base.logger.log(Level.FINE,"Setting temperature to " + temp + "C");

		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.SET_TEMP.getCode());
		pb.add8((byte) 2); // payload length
		pb.add16(temp);
		runCommand(pb.getPacket());

		super.setTemperature(temperature);
	}

	public void readTemperature() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.GET_TEMP.getCode());
		PacketResponse pr = runCommand(pb.getPacket());

		int temp = pr.get16();
		machine.currentTool().setCurrentTemperature(temp);

		Base.logger.log(Level.FINE,"Current temperature: "
					+ machine.currentTool().getCurrentTemperature() + "C");

		super.readTemperature();
	}

	/***************************************************************************
	 * Platform Temperature interface functions
	 **************************************************************************/
	public void setPlatformTemperature(double temperature) {
		// constrain our temperature.
		int temp = (int) Math.round(temperature);
		temp = Math.min(temp, 65535);
		
		Base.logger.log(Level.FINE,"Setting platform temperature to " + temp + "C");
		
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.SET_PLATFORM_TEMP.getCode());
		pb.add8((byte) 2); // payload length
		pb.add16(temp);
		runCommand(pb.getPacket());
		
		super.setPlatformTemperature(temperature);
	}
	
	public void readPlatformTemperature() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.GET_PLATFORM_TEMP.getCode());
		PacketResponse pr = runCommand(pb.getPacket());
		
		int temp = pr.get16();
		machine.currentTool().setPlatformCurrentTemperature(temp);
		
		Base.logger.log(Level.FINE,"Current platform temperature: "
						+ machine.currentTool().getPlatformCurrentTemperature() + "C");
		
		super.readPlatformTemperature();
	}

	/***************************************************************************
	 * Flood Coolant interface functions
	 **************************************************************************/
	public void enableFloodCoolant() {
		// TODO: throw unsupported exception

		super.enableFloodCoolant();
	}

	public void disableFloodCoolant() {
		// TODO: throw unsupported exception

		super.disableFloodCoolant();
	}

	/***************************************************************************
	 * Mist Coolant interface functions
	 **************************************************************************/
	public void enableMistCoolant() {
		// TODO: throw unsupported exception

		super.enableMistCoolant();
	}

	public void disableMistCoolant() {
		// TODO: throw unsupported exception

		super.disableMistCoolant();
	}

	/***************************************************************************
	 * Fan interface functions
	 **************************************************************************/
	public void enableFan() {
		Base.logger.log(Level.FINE,"Enabling fan");

		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.TOGGLE_FAN.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8((byte) 1); // enable
		runCommand(pb.getPacket());

		super.enableFan();
	}

	public void disableFan() {
		Base.logger.log(Level.FINE,"Disabling fan");

		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.TOGGLE_FAN.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8((byte) 0); // disable
		runCommand(pb.getPacket());

		super.disableFan();
	}

	/***************************************************************************
	 * Valve interface functions
	 **************************************************************************/
	public void openValve() {
		Base.logger.log(Level.FINE,"Opening valve");

		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.TOGGLE_VALVE.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8((byte) 1); // enable
		runCommand(pb.getPacket());

		super.openValve();
	}

	public void closeValve() {
		Base.logger.log(Level.FINE,"Closing valve");

		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_COMMAND.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.TOGGLE_VALVE.getCode());
		pb.add8((byte) 1); // payload length
		pb.add8((byte) 0); // disable
		runCommand(pb.getPacket());

		super.closeValve();
	}

	/***************************************************************************
	 * Collet interface functions
	 **************************************************************************/
	public void openCollet() {
		// TODO: throw unsupported exception.

		super.openCollet();
	}

	public void closeCollet() {
		// TODO: throw unsupported exception.

		super.closeCollet();
	}

	/***************************************************************************
	 * Pause/unpause functionality for asynchronous devices
	 **************************************************************************/
	public void pause() {
		Base.logger.log(Level.FINE,"Sending asynch pause command");
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.PAUSE.getCode());
		runCommand(pb.getPacket());
	}

	public void unpause() {
		Base.logger.log(Level.FINE,"Sending asynch unpause command");
		// There is no explicit unpause command on the Sanguino3G; instead we
		// use
		// the pause command to toggle the pause state.
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.PAUSE.getCode());
		runCommand(pb.getPacket());
	}

	/***************************************************************************
	 * Various timer and math functions.
	 **************************************************************************/

	private Point3d getDeltaDistance(Point3d current, Point3d target) {
		// calculate our deltas.
		Point3d delta = new Point3d();
		delta.x = target.x - current.x;
		delta.y = target.y - current.y;
		delta.z = target.z - current.z;

		return delta;
	}

	@SuppressWarnings("unused")
	private Point3d getDeltaSteps(Point3d current, Point3d target) {
		return machine.mmToSteps(getDeltaDistance(current, target));
	}

	private Point3d getAbsDeltaDistance(Point3d current, Point3d target) {
		// calculate our deltas.
		Point3d delta = new Point3d();
		delta.x = Math.abs(target.x - current.x);
		delta.y = Math.abs(target.y - current.y);
		delta.z = Math.abs(target.z - current.z);

		return delta;
	}

	private Point3d getAbsDeltaSteps(Point3d current, Point3d target) {
		return machine.mmToSteps(getAbsDeltaDistance(current, target));
	}

	private long convertFeedrateToMicros(Point3d current, Point3d target,
			double feedrate) {

		Point3d deltaDistance = getAbsDeltaDistance(current, target);
		Point3d deltaSteps = getAbsDeltaSteps(current, target);

		// System.out.println("current: " + current);
		// System.out.println("target: " + target);
		// System.out.println("deltas:" + deltaDistance);

		// try {
		// Thread.sleep(10000);
		// } catch (Exception e) {}

		// how long is our line length?
		double distance = Math.sqrt(deltaDistance.x * deltaDistance.x
				+ deltaDistance.y * deltaDistance.y + deltaDistance.z
				* deltaDistance.z);

		double masterSteps = getLongestLength(deltaSteps);

		// distance is in steps
		// feedrate is in steps/
		// distance / feedrate * 60,000,000 = move duration in microseconds
		double micros = distance / feedrate * 60000000.0;

		// micros / masterSteps = time between steps for master axis.
		double step_delay = micros / masterSteps;

		// System.out.println("Distance: " + distance);
		// System.out.println("Feedrate: " + feedrate);
		// System.out.println("Micros: " + micros);
		// System.out.println("Master steps:" + masterSteps);
		// System.out.println("Step Delay (micros): " + step_delay);

		return (long) Math.round(step_delay);
	}

	private double getLongestLength(Point3d p) {
		// find the dominant axis.
		if (p.x > p.y) {
			if (p.z > p.x)
				return p.z;
			else
				return p.x;
		} else {
			if (p.z > p.y)
				return p.z;
			else
				return p.y;
		}
	}

	public String getDriverName() {
		return "Sanguino3G";
	}

	/***************************************************************************
	 * Stop and system state reset
	 **************************************************************************/
	public void stop() {
		System.out.println("Stop.");
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.ABORT.getCode());
		runCommand(pb.getPacket());
		// invalidate position, force reconciliation.
		invalidatePosition();
	}

	protected Point3d reconcilePosition() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.GET_POSITION.getCode());
		PacketResponse pr = runCommand(pb.getPacket());
		Point3d steps = new Point3d(pr.get32(), pr.get32(), pr.get32());
		return machine.stepsToMM(steps);
	}

	public void reset() {
		System.out.println("Reset.");
		setInitialized(false);
		if (version.compareTo(new Version(1,4)) >= 0) {
			// WDT reset introduced in version 1.4 firmware
			PacketBuilder pb = new PacketBuilder(CommandCodeMaster.RESET.getCode());
			runCommand(pb.getPacket());
		}
		initialize();
	}

	private boolean eepromChecked = false;
	private static final int EEPROM_CHECK_LOW = 0x5A;
	private static final int EEPROM_CHECK_HIGH = 0x78;
	
	private void checkEEPROM() {
		if (!eepromChecked) {
			byte versionBytes[] = readFromEEPROM(EEPROM_CHECK_OFFSET,2);
			if ((versionBytes[0] != EEPROM_CHECK_LOW) ||
				(versionBytes[1] != EEPROM_CHECK_HIGH)) {
				System.err.println("Cleaning EEPROM");
				// Wipe EEPROM
				byte eepromWipe[] = new byte[16];
				Arrays.fill(eepromWipe,(byte)0x00);
				eepromWipe[0] = EEPROM_CHECK_LOW;
				eepromWipe[1] = EEPROM_CHECK_HIGH;
				writeToEEPROM(0,eepromWipe);
				Arrays.fill(eepromWipe,(byte)0x00);
				for (int i = 16; i < 256; i+=16) {
					writeToEEPROM(i,eepromWipe);
				}
			}
			eepromChecked = true;
		}
	}
	
	private void writeToEEPROM(int offset, byte[] data) {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.WRITE_EEPROM.getCode());
		pb.add16(offset);
		pb.add8(data.length);
		for (byte b : data) {
			pb.add8(b);
		}
		PacketResponse pr = runCommand(pb.getPacket());
		assert pr.get8() == data.length; 
	}

	private byte[] readFromToolEEPROM(int offset, int len) {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		pb.add8((byte) machine.currentTool().getIndex());
		pb.add8(CommandCodeSlave.READ_FROM_EEPROM.getCode());
		pb.add16(offset);
		pb.add8(len);
		PacketResponse pr = runCommand(pb.getPacket());
		if (pr.isOK()) {
			int rvlen = Math.min(pr.getPayload().length - 1,len);
			byte[] rv = new byte[rvlen];
			// Copy removes the first response byte from the packet payload.
			System.arraycopy(pr.getPayload(),1,rv,0,rvlen);
			return rv;
		}
		return null;
	}
	
	private void writeToToolEEPROM(int offset, byte[] data) {
		final int MAX_PAYLOAD = 11;
		while (data.length > MAX_PAYLOAD) {
			byte[] head = new byte[MAX_PAYLOAD];
			byte[] tail = new byte[data.length-MAX_PAYLOAD];
			System.arraycopy(data,0,head,0,MAX_PAYLOAD);
			System.arraycopy(data,MAX_PAYLOAD,tail,0,data.length-MAX_PAYLOAD);
			writeToToolEEPROM(offset, head);
			offset += MAX_PAYLOAD;
			data = tail;
		}
		PacketBuilder slavepb = new PacketBuilder(CommandCodeMaster.TOOL_QUERY.getCode());
		slavepb.add8((byte) machine.currentTool().getIndex());
		slavepb.add8(CommandCodeSlave.WRITE_TO_EEPROM.getCode());
		slavepb.add16(offset);
		slavepb.add8(data.length);
		for (byte b : data) {
			slavepb.add8(b);
		}
		PacketResponse slavepr = runCommand(slavepb.getPacket());
		assert slavepr.get8() == data.length; 
	}

	private byte[] readFromEEPROM(int offset, int len) {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.READ_EEPROM.getCode());
		pb.add16(offset);
		pb.add8(len);
		PacketResponse pr = runCommand(pb.getPacket());
		if (pr.isOK()) {
			int rvlen = Math.min(pr.getPayload().length - 1,len);
			byte[] rv = new byte[rvlen];
			// Copy removes the first response byte from the packet payload.
			System.arraycopy(pr.getPayload(),1,rv,0,rvlen);
			return rv;
		}
		return null;
	}
	
	/// EEPROM map:
	/// 00-01 - EEPROM data version
	/// 02    - Axis inversion byte
	/// 32-47 - Machine name (max. 16 chars)
	final private static int EEPROM_CHECK_OFFSET = 0;
	final private static int EEPROM_MACHINE_NAME_OFFSET = 32;
	final private static int EEPROM_AXIS_INVERSION_OFFSET = 2;

	final static class ECThermistorOffsets {
		final private static int[] TABLE_OFFSETS = {
			0x00f0,
			0x0170
		};

		final private static int R0 = 0x00;
		final private static int T0 = 0x04;
		final private static int BETA = 0x08;
		final private static int DATA = 0x10;
		
		public static int r0(int which) { return R0 + TABLE_OFFSETS[which]; }
		public static int t0(int which) { return T0 + TABLE_OFFSETS[which]; }
		public static int beta(int which) { return BETA + TABLE_OFFSETS[which]; }
		public static int data(int which) { return DATA + TABLE_OFFSETS[which]; }
	};	

	final private static int MAX_MACHINE_NAME_LEN = 16;
	public EnumSet<Axis> getInvertedParameters() {
		checkEEPROM();
		byte[] b = readFromEEPROM(EEPROM_AXIS_INVERSION_OFFSET,1);
		EnumSet<Axis> r = EnumSet.noneOf(Axis.class);
		if ( (b[0] & (0x01 << 0)) != 0 ) r.add(Axis.X);
		if ( (b[0] & (0x01 << 1)) != 0 ) r.add(Axis.Y);
		if ( (b[0] & (0x01 << 2)) != 0 ) r.add(Axis.Z);
		return r;
	}

	public void setInvertedParameters(EnumSet<Axis> axes) {
		byte b[] = new byte[1];
		if (axes.contains(Axis.X)) b[0] = (byte)(b[0] | (0x01 << 0));
		if (axes.contains(Axis.Y)) b[0] = (byte)(b[0] | (0x01 << 1));
		if (axes.contains(Axis.Z)) b[0] = (byte)(b[0] | (0x01 << 2));
		writeToEEPROM(EEPROM_AXIS_INVERSION_OFFSET,b);
	}

	public String getMachineName() {
		checkEEPROM();
		byte[] data = readFromEEPROM(EEPROM_MACHINE_NAME_OFFSET,MAX_MACHINE_NAME_LEN);
		try {
			int len = 0;
			while (len < MAX_MACHINE_NAME_LEN && data[len] != 0) len++;
			return new String(data,0,len,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setMachineName(String machineName) {
		machineName = new String(machineName);
		if (machineName.length() > 16) { 
			machineName = machineName.substring(0,16);
		}
		byte b[] = new byte[16];
		int idx = 0;
		for (byte sb : machineName.getBytes()) {
			b[idx++] = sb;
			if (idx == 16) break;
		}
		if (idx < 16) b[idx] = 0;
		writeToEEPROM(EEPROM_MACHINE_NAME_OFFSET,b);
	}
	
	public boolean hasFeatureOnboardParameters() {
		return version.compareTo(new Version(1,2)) >= 0; 
	}

	public void createThermistorTable(int which, double r0, double t0, double beta) {
		// Generate a thermistor table for r0 = 100K.
		final int ADC_RANGE = 1024;
		final int NUMTEMPS = 20;
		byte table[] = new byte[NUMTEMPS*2*2];
		class ThermistorConverter {
			final double ZERO_C_IN_KELVIN = 273.15;
			public double vadc,rs,vs,k,beta;
			public ThermistorConverter(double r0, double t0C, double beta, double r2) {
				this.beta = beta;
				this.vs = this.vadc = 5.0;
				final double t0K = ZERO_C_IN_KELVIN + t0C;
				this.k = r0 * Math.exp(-beta / t0K);
				this.rs = r2;		
			}
			public double temp(double adc) {
				// Convert ADC reading into a temperature in Celsius
				double v = adc * this.vadc / ADC_RANGE;
				double r = this.rs * v / (this.vs - v);
				return (this.beta / Math.log(r / this.k)) - ZERO_C_IN_KELVIN;
			}
		};
		ThermistorConverter tc = new ThermistorConverter(r0,t0,beta,4700.0);
		double adc = 1; // matching the python script's choices for now;
		// we could do better with this distribution.
		for (int i = 0; i < NUMTEMPS; i++) {
			double temp = tc.temp(adc);
			// extruder controller is little-endian
			int tempi = (int)temp;
			int adci = (int)adc;
			System.err.println("{ "+Integer.toString(adci) +"," +Integer.toString(tempi)+" }");
			table[(2*2*i)+0] = (byte)(adci & 0xff); // ADC low
			table[(2*2*i)+1] = (byte)(adci >> 8); // ADC high
			table[(2*2*i)+2] = (byte)(tempi & 0xff); // temp low
			table[(2*2*i)+3] = (byte)(tempi >> 8); // temp high
			adc += (ADC_RANGE/(NUMTEMPS-1));
		}
		// Add indicators
		byte eepromIndicator[] = new byte[2];
		eepromIndicator[0] = EEPROM_CHECK_LOW;
		eepromIndicator[1] = EEPROM_CHECK_HIGH;
		writeToToolEEPROM(0,eepromIndicator);

		writeToToolEEPROM(ECThermistorOffsets.beta(which),intToLE((int)beta));
		writeToToolEEPROM(ECThermistorOffsets.r0(which),intToLE((int)r0));
		writeToToolEEPROM(ECThermistorOffsets.t0(which),intToLE((int)t0));
		writeToToolEEPROM(ECThermistorOffsets.data(which),table);
	}

	private byte[] intToLE(int s, int sz) {
		byte buf[] = new byte[sz];
		for (int i = 0; i < sz; i++) {
			buf[i] = (byte)(s & 0xff);
			s = s >>> 8;
		}
		return buf;
	}

	private byte[] intToLE(int s) {
		return intToLE(s,4);
	}

	ResponseCode convertSDCode(int code) {
		switch (code) {
		case 0:
			return ResponseCode.SUCCESS;
		case 1:
			return ResponseCode.FAIL_NO_CARD;
		case 2:
			return ResponseCode.FAIL_INIT;
		case 3:
			return ResponseCode.FAIL_PARTITION;
		case 4:
			return ResponseCode.FAIL_FS;
		case 5:
			return ResponseCode.FAIL_ROOT_DIR;
		case 6:
			return ResponseCode.FAIL_LOCKED;
		case 7:
			return ResponseCode.FAIL_NO_FILE;
		default:
		}
		return ResponseCode.FAIL_GENERIC;
	}

	FileOutputStream fileCaptureOstream = null;
	
	public void beginFileCapture(String path) throws FileNotFoundException {
		fileCaptureOstream = new FileOutputStream(new File(path));
	}
	
	public void endFileCapture() throws IOException {
		fileCaptureOstream.close();
		fileCaptureOstream = null;
	}
	
	public ResponseCode beginCapture(String filename) {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.CAPTURE_TO_FILE.getCode());
		for (byte b : filename.getBytes()) {
			pb.add8(b);
		}
		pb.add8(0); // null-terminate string
		PacketResponse pr = runCommand(pb.getPacket());
		return convertSDCode(pr.get8());
	}

	public int endCapture() {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.END_CAPTURE.getCode());
		PacketResponse pr = runCommand(pb.getPacket());
		return pr.get32();
	}

	public ResponseCode playback(String filename) {
		PacketBuilder pb = new PacketBuilder(CommandCodeMaster.PLAYBACK_CAPTURE.getCode());
		for (byte b : filename.getBytes()) {
			pb.add8(b);
		}
		pb.add8(0); // null-terminate string
		PacketResponse pr = runCommand(pb.getPacket());
		return convertSDCode(pr.get8());
	}

	public boolean hasFeatureSDCardCapture() {
		return version.compareTo(new Version(1,3)) >= 0; 
	}
	
	public List<String> getFileList() {
		Vector<String> fileList = new Vector<String>();
		boolean reset = true;
		while (true) {
			PacketBuilder pb = new PacketBuilder(CommandCodeMaster.NEXT_FILENAME.getCode());
			pb.add8(reset?1:0);
			reset = false;
			PacketResponse pr = runCommand(pb.getPacket());
			ResponseCode rc = convertSDCode(pr.get8());
			if (rc != ResponseCode.SUCCESS) {
				return fileList;
			}
			StringBuffer s = new StringBuffer();
			while (true) {
				char c = (char)pr.get8();
				if (c == 0) break;
				s.append(c);
			}
			if (s.length() == 0) break;
			fileList.add(s.toString());
		}
		return fileList;
	}

	public int getBeta(int which) {
		byte r[] = readFromToolEEPROM(ECThermistorOffsets.beta(which),4);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}

	public int getR0(int which) {
		byte r[] = readFromToolEEPROM(ECThermistorOffsets.r0(which),4);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}

	public int getT0(int which) {
		byte r[] = readFromToolEEPROM(ECThermistorOffsets.t0(which),4);
		int val = 0;
		for (int i = 0; i < 4; i++) {
			val = val + (((int)r[i] & 0xff) << 8*i);
		}
		return val;
	}

	final static class ECBackoffOffsets {
		/// Backoff stop time, in ms: 2 bytes
		final static int STOP_MS = 0x0004;
		/// Backoff reverse time, in ms: 2 bytes
		final static int REVERSE_MS = 0x0006;
		/// Backoff forward time, in ms: 2 bytes
		final static int FORWARD_MS = 0x0008;
		/// Backoff trigger time, in ms: 2 bytes
		final static int TRIGGER_MS = 0x000A;
	};

	private int read16FromToolEEPROM(int offset, int defaultValue) {
		byte r[] = readFromToolEEPROM(offset,2);
		int val = r[0]&0xff + ((r[1]&0xff)<<8);
		if (val == 0x0ffff) return defaultValue;
		return val;
	}
	
	public BackoffParameters getBackoffParameters() {
		BackoffParameters bp = new BackoffParameters();
		bp.forwardMs = read16FromToolEEPROM(ECBackoffOffsets.FORWARD_MS, 300);
		bp.stopMs = read16FromToolEEPROM(ECBackoffOffsets.STOP_MS, 5);
		bp.reverseMs = read16FromToolEEPROM(ECBackoffOffsets.REVERSE_MS, 500);
		bp.triggerMs = read16FromToolEEPROM(ECBackoffOffsets.TRIGGER_MS, 300);
		return bp;
	}
	
	public void setBackoffParameters(BackoffParameters bp) {
		writeToToolEEPROM(ECBackoffOffsets.FORWARD_MS,intToLE(bp.forwardMs,2));
		writeToToolEEPROM(ECBackoffOffsets.STOP_MS,intToLE(bp.stopMs,2));
		writeToToolEEPROM(ECBackoffOffsets.REVERSE_MS,intToLE(bp.reverseMs,2));
		writeToToolEEPROM(ECBackoffOffsets.TRIGGER_MS,intToLE(bp.triggerMs,2));
	}
}
