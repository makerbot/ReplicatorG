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

package replicatorg.app.drivers;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import javax.vecmath.Point3d;

import org.w3c.dom.Node;

import replicatorg.app.Preferences;
import replicatorg.app.Serial;
import replicatorg.app.exceptions.SerialException;
import replicatorg.app.models.ToolModel;
import replicatorg.app.tools.XML;

public class Sanguino3GDriver extends DriverBaseImplementation
{

    class Target {
	public final static int THREE_AXIS  =0;
	public final static int EXTRUDER    =1;
    };

    class CommandCodes3Axis {
	public final static int GET_VERSION     =   0;
	public final static int INIT            =   1;
	public final static int GET_AVAIL_BUF   =   2;
	public final static int CLEAR_BUF       =   3;
	public final static int GET_POS         =   4;
	public final static int GET_RANGE       =   5;
	public final static int SET_RANGE       =   6;
	public final static int ABORT           =   7;
	public final static int PAUSE           =   8;
	public final static int PROBE           =   9;
	public final static int QUEUE_POINT_INC = 128;
	public final static int QUEUE_POINT_ABS = 129;
	public final static int SET_POS         = 130;
	public final static int FIND_MINS       = 131;
	public final static int FIND_MAXS       = 132;
	public final static int DELAY           = 133;
	public final static int CHANGE_TOOL     = 135;
	public final static int WAIT_FOR_TOOL   = 136;
    };

    /**
     * this is if we need to talk over serial
     */
    private Serial serial;
    
    /**
     * Serial connection parameters
     **/
    String name;
    int    rate;
    char   parity;
    int    databits;
    float  stopbits;
    
    /**
     * Java implementation of the IButton/Maxim 8-bit CRC.
     * Code ported from the AVR-libc implementation, which is used
     * on the RR3G end.
     */
    protected class IButtonCrc {

	private byte crc = 0;

	/**
	 * Construct a new, initialized object for keeping track of a CRC.
	 */
	public IButtonCrc() {
	}

	/**
	 * Update the  CRC with a new byte of sequential data.
	 * See include/util/crc16.h in the avr-libc project for a 
	 * full explanation of the algorithm.
	 * @param data a byte of new data to be added to the crc.
	 */
	public void update(byte data) {
	    crc = (byte)(crc ^ data); // i loathe java's promotion rules
	    for (int i=0; i<8; i++) {
		if ((crc & 0x01) != 0) {
		    crc = (byte)((crc >>> 1) ^ 0x8c);
		} else {
		    crc = (byte)(crc >>> 1);
		}
	    }
	}

	/**
	 * Get the 8-bit crc value.
	 */
	public byte getCrc() {
	    return crc;
	}

	/**
	 * Reset the crc.
	 */
	public void reset() {
	    crc = 0;
	}
    }

    private final byte START_BYTE = (byte)0xD5;


    class ResponseCode {
	final static int GENERIC_ERROR   =0;
	final static int OK              =1;
	final static int BUFFER_OVERFLOW =2;
	final static int CRC_MISMATCH    =3;
	final static int QUERY_OVERFLOW  =4;
    };

    /// Build a new packet, with target and command information.
    class PacketBuilder {
	// yay magic numbers.
	byte[] data = new byte[512];
	int idx = 2;
	IButtonCrc crc = new IButtonCrc();

	PacketBuilder( int target, int command ) {
	    data[0] = START_BYTE;
	    add8((byte)target);
	    add8((byte)command);
	}

	void add8( byte v ) {
	    data[idx++] =  v;
	    crc.update(v);
	}
	void add16( int v ) {
	    add8((byte)(v&0xff));
	    add8((byte)((v>>8)&0xff));
	}
	void add32( int v ) {
	    add16(v&0xffff);
	    add16((v>>16)&0xffff);
	}
	byte[] getPacket() {
	    data[idx] = crc.getCrc();
	    data[1] = (byte)(idx-2); // len does not count packet header
	    byte[] rv = new byte[idx+1];
	    System.arraycopy(data,0,rv,0,idx+1);
	    return rv;
	}
    };

    class PacketProcessor {
	// not on java 5 yet.
	final static byte PS_START = 0;
	final static byte PS_LEN = 1;
	final static byte PS_PAYLOAD = 2;
	final static byte PS_CRC = 3;
	final static byte PS_LAST = 4;

	byte packetState = PS_START;
	int payloadLength = -1;
	int payloadIdx = 0;
	byte[] payload;
	byte targetCrc = 0;
	IButtonCrc crc;

	public void reset() {
	    packetState = PS_START;
	}

	public PacketResponse getResponse() { return new PacketResponse(payload); }

	public boolean processByte(byte b) {
	    System.err.println("IN: Processing byte " + Integer.toHexString(b));
	    switch (packetState) {
	    case PS_START:
		if (b == START_BYTE) {
		    packetState = PS_LEN;
		} else {
		    // throw exception?
		}
		break;
	    case PS_LEN:
		payloadLength = ((int)b) & 0xFF;
		payload = new byte[payloadLength];
		crc = new IButtonCrc();
		packetState = PS_PAYLOAD;
		break;
	    case PS_PAYLOAD:
		// sanity check
		if (payloadIdx < payloadLength) {
		    payload[payloadIdx++] = b;
		    crc.update(b);
		}
		if (payloadIdx >= payloadLength) {
		    packetState = PS_CRC;
		}
		break;
	    case PS_CRC:
		targetCrc = b;
		if (crc.getCrc() != targetCrc) {
		    throw new java.lang.RuntimeException("CRC mismatch on reply");
		}
		return true;
	    }
	    return false;
	}
    }

    class PacketResponse {
	byte[] payload;
	int readPoint = 1;
	public PacketResponse(byte[] p) {
	    payload = p;
	}
	void printDebug() {
	    String msg = "Unknown";
	    switch(payload[0]) {
	    case ResponseCode.GENERIC_ERROR:
		msg = "Generic Error";
		break;
	    case ResponseCode.OK:
		msg = "OK";
		break;
	    case ResponseCode.BUFFER_OVERFLOW:
		msg = "Buffer overflow";
		break;
	    case ResponseCode.CRC_MISMATCH:
		msg = "CRC mismatch";
		break;
	    case ResponseCode.QUERY_OVERFLOW:
		msg = "Query overflow";
		break;
	    }
	    System.err.println("Packet response code: "+msg);
	    System.err.print("Packet payload: ");
	    for (int i = 1; i < payload.length; i++) {
		System.err.print(Integer.toHexString(payload[i]) + " ");
	    }
	    System.err.print("\n");
	}

	int get8() {
	    return payload[readPoint++];
	}
	int get16() {
	    return get8() + (get8()<<8);
	}
	int get32() {
	    return get16() + (get16()<<16);
	}

	public boolean isOK() { 
	    return payload[0] == ResponseCode.OK;
	}
    };


    public Sanguino3GDriver()
    {
	super();
	
	//init our variables.
	setInitialized(false);
		
	//some decent default prefs.
	String[] serialPortNames = Serial.list();
	if (serialPortNames.length != 0)
	    name = serialPortNames[0];
	else
	    name = null;
		
	rate = Preferences.getInteger("serial.debug_rate");
	parity = Preferences.get("serial.parity").charAt(0);
	databits = Preferences.getInteger("serial.databits");
	stopbits = new Float(Preferences.get("serial.stopbits")).floatValue();
    }
	
    public void loadXML(Node xml)
    {
	super.loadXML(xml);
		
	//load from our XML config, if we have it.
	if (XML.hasChildNode(xml, "portname"))
	    name = XML.getChildNodeValue(xml, "portname");
	if (XML.hasChildNode(xml, "rate"))
	    rate = Integer.parseInt(XML.getChildNodeValue(xml, "rate"));
	if (XML.hasChildNode(xml, "parity"))
	    parity = XML.getChildNodeValue(xml, "parity").charAt(0);
	if (XML.hasChildNode(xml, "databits"))
	    databits = Integer.parseInt(XML.getChildNodeValue(xml, "databits"));
	if (databits != 8) {
	    throw new java.lang.RuntimeException("Sanguino3G driver requires 8 serial data bits.");
	}
	if (XML.hasChildNode(xml, "stopbits"))
	    stopbits = Integer.parseInt(XML.getChildNodeValue(xml, "stopbits"));
    }
	
    public void initialize()
    {
	// Create our serial object
	if (serial == null) {
	    if (name != null) {
		try {
		    System.out.println("Connecting to " + name + " at " + rate);
		    serial = new Serial(name, rate, parity, databits, stopbits);
		} catch (SerialException e) {
		    System.out.println("Unable to open port " + name + "\n");
		    return;
		}
	    } else {
		System.out.println("No Serial Port found.\n");
		return;
	    }
	}
	//wait till we're initialized
	if (!isInitialized()) {
	    // attempt to send version command and retrieve reply.
	    try {

		
	    } catch (Exception e) {
		    //todo: handle init exceptions here
	    }
	    System.out.println("Ready to rock.");
	}
	
	//default us to absolute positioning
	// todo agm sendCommand("G90");
    }
		
    /**
     * Sends the command over the serial connection and retrieves a result.
     */
    protected PacketResponse runCommand(byte[] packet)
    {
	assert (isInitialized());
	assert (serial != null);

	if (packet == null || packet.length < 4) return null; // skip empty commands or broken commands
	boolean checkQueue = false;
	if (packet[2] == 0x0 && (packet[3]&0x80) != 0x0) {
	    checkQueue = true;
	}

	synchronized(serial) {
	    //do the actual send.
	    serial.write(packet);
	}
	System.out.println("OUT:  Target " + Integer.toHexString(packet[2])+ " cmd " + Integer.toHexString(packet[3]) );
	PacketProcessor pp = new PacketProcessor();
	try {
	    while (!pp.processByte((byte)serial.input.read())) {}
	} catch (java.io.IOException ioe) {
	    System.err.println(ioe.toString());
	}
	return pp.getResponse();
    }
	
	
    public boolean isFinished()
    {
	// todo agm
	return true;
    }
	
    public void dispose()
    {
	super.dispose();
		
	if (serial != null) serial.dispose();
	serial = null;
    }
	
    /****************************************************
     *  commands for interfacing with the driver directly
     ****************************************************/
    public int getVersion(int ourVersion) {
	PacketBuilder pb = new PacketBuilder(Target.THREE_AXIS,CommandCodes3Axis.GET_VERSION);
	pb.add16(0xbeef);
	PacketResponse pr = runCommand(pb.getPacket());
	int version = pr.get16();
	pr.printDebug();
	System.out.println("Reported version: " + Integer.toHexString(version));
	return version;
    }

	
    public void queuePoint(Point3d p)
    {
	//String cmd = "G1 X" + df.format(p.x) + " Y" + df.format(p.y) + " Z" + df.format(p.z) + " F" + df.format(getCurrentFeedrate());
		
	//sendCommand(cmd);
		
	super.queuePoint(p);
    }
	
    public void setCurrentPosition(Point3d p)
    {
	//sendCommand("G92 X" + df.format(p.x) + " Y" + df.format(p.y) + " Z" + df.format(p.z));

	super.setCurrentPosition(p);
    }
	
    public void homeXYZ()
    {
	//sendCommand("G28 XYZ");
		
	super.homeXYZ();
    }

    public void homeXY()
    {
	//sendCommand("G28 XY");

	super.homeXY();
    }

    public void homeX()
    {
	//sendCommand("G28 X");

	super.homeX();
    }

    public void homeY()
    {
	//sendCommand("G28 Y");
	
	super.homeY();
    }

    public void homeZ()
    {
	//sendCommand("G28 Z");
		
	super.homeZ();
    }
	
    public void delay(long millis)
    {
	int seconds = Math.round(millis/1000);

	//sendCommand("G4 P" + seconds);
		
	//no super call requried.
    }
	
    public void openClamp(int clampIndex)
    {
	//sendCommand("M11 Q" + clampIndex);
		
	super.openClamp(clampIndex);
    }
	
    public void closeClamp(int clampIndex)
    {
	//sendCommand("M10 Q" + clampIndex);
		
	super.closeClamp(clampIndex);
    }
	
    public void enableDrives()
    {
	//sendCommand("M17");
		
	super.enableDrives();
    }
	
    public void disableDrives()
    {
	//sendCommand("M18");

	super.disableDrives();
    }
	
    public void changeGearRatio(int ratioIndex)
    {
	//gear ratio codes are M40-M46
	int code = 40 + ratioIndex;
	code = Math.max(40, code);
	code = Math.min(46, code);
		
	//sendCommand("M" + code);
		
	super.changeGearRatio(ratioIndex);
    }
	
    private String _getToolCode()
    {
	return "T" + machine.currentTool().getIndex() + " ";
    }

    /*************************************
     *  Motor interface functions
     *************************************/
    public void setMotorSpeed(double rpm)
    {
	//sendCommand(_getToolCode() + "M108 S" + df.format(rpm));

	super.setMotorSpeed(rpm);
    }
	
    public void enableMotor()
    {
	String command = _getToolCode();

	if (machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE)
	    command += "M101";
	else
	    command += "M102";

	//sendCommand(command);

	super.enableMotor();
    }
	
    public void disableMotor()
    {
	//sendCommand(_getToolCode() + "M103");

	super.disableMotor();
    }

    /*************************************
     *  Spindle interface functions
     *************************************/
    public void setSpindleSpeed(double rpm)
    {
	//sendCommand(_getToolCode() + "S" + df.format(rpm));

	super.setSpindleSpeed(rpm);
    }
	
    public void enableSpindle()
    {
	String command = _getToolCode();

	if (machine.currentTool().getSpindleDirection() == ToolModel.MOTOR_CLOCKWISE)
	    command += "M3";
	else
	    command += "M4";

	//sendCommand(command);
		
	super.enableSpindle();
    }
	
    public void disableSpindle()
    {
	//sendCommand(_getToolCode() + "M5");

	super.disableSpindle();
    }
	
    public void readSpindleSpeed()
    {
	//sendCommand(_getToolCode() + "M50");
		
	super.readSpindleSpeed();
    }
	
    /*************************************
     *  Temperature interface functions
     *************************************/
    public void setTemperature(double temperature)
    {
	//sendCommand(_getToolCode() + "M104 S" + df.format(temperature));
		
	super.setTemperature(temperature);
    }

    public void readTemperature()
    {
	//sendCommand(_getToolCode() + "M105");
		
	super.readTemperature();
    }

    /*************************************
     *  Flood Coolant interface functions
     *************************************/
    public void enableFloodCoolant()
    {
	//sendCommand(_getToolCode() + "M7");
		
	super.enableFloodCoolant();
    }
	
    public void disableFloodCoolant()
    {
	//sendCommand(_getToolCode() + "M9");
		
	super.disableFloodCoolant();
    }

    /*************************************
     *  Mist Coolant interface functions
     *************************************/
    public void enableMistCoolant()
    {
	//sendCommand(_getToolCode() + "M8");
		
	super.enableMistCoolant();
    }
	
    public void disableMistCoolant()
    {
	//sendCommand(_getToolCode() + "M9");

	super.disableMistCoolant();
    }

    /*************************************
     *  Fan interface functions
     *************************************/
    public void enableFan()
    {
	//sendCommand(_getToolCode() + "M106");
		
	super.enableFan();
    }
	
    public void disableFan()
    {
	//sendCommand(_getToolCode() + "M107");
		
	super.disableFan();
    }
	
    /*************************************
     *  Valve interface functions
     *************************************/
    public void openValve()
    {
	//sendCommand(_getToolCode() + "M126");
		
	super.openValve();
    }
	
    public void closeValve()
    {
	//sendCommand(_getToolCode() + "M127");
		
	super.closeValve();
    }
	
    /*************************************
     *  Collet interface functions
     *************************************/
    public void openCollet()
    {
	//sendCommand(_getToolCode() + "M21");
		
	super.openCollet();
    }
	
    public void closeCollet()
    {
	//sendCommand(_getToolCode() + "M22");
		
	super.closeCollet();
    }
	
}
