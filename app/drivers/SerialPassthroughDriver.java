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

package processing.app.drivers;

import processing.app.*;
import processing.app.exceptions.*;
import processing.core.*;

import gnu.io.*;
import java.util.*;
import org.w3c.dom.*;
import javax.vecmath.*;

public class SerialPassthroughDriver extends DriverBaseImplementation
{
	/**
	* this is if we need to talk over serial
	*/
	private Serial serial;

	/**
	* our array of gcode commands
	*/
	private Vector commands;
	
	/**
	* our pointer to the currently executing command
	*/
	private int currentCommand = 0;
	
	/**
	* the size of the buffer on the GCode host
	*/
	private int maxBufferSize = 128;
	
	/**
	* the amount of data we've sent and is in the buffer.
	*/
	private int bufferSize = 0;
	
	/**
	* how many commands do we have in the buffer?
	*/
	private int bufferLength = 0;
	
	/**
	* What did we get back from serial?
	*/
	private String result = "";
	
	/**
	* Serial connection parameters
	**/
	String name;
	int    rate;
	char   parity;
	int    databits;
	float  stopbits;
	
	public SerialPassthroughDriver()
	{
		super();
		
		//init our variables.
		commands = new Vector();
		bufferSize = 0;
		bufferLength = 0;
		currentCommand = 0;
		isInitialized = false;
		
		//some decent default prefs.
		name = Serial.list()[0];
		rate = Preferences.getInteger("serial.debug_rate");
		parity = Preferences.get("serial.parity").charAt(0);
		databits = Preferences.getInteger("serial.databits");
		stopbits = new Float(Preferences.get("serial.stopbits")).floatValue();
	}
	
	public void loadXML(Node xml)
	{
		//load from our XML config, if we have it.
		if (Base.hasChildNode(xml, "portname"))
			name = Base.getChildNodeValue(xml, "portname");
		if (Base.hasChildNode(xml, "rate"))
			rate = Integer.parseInt(Base.getChildNodeValue(xml, "rate"));
		if (Base.hasChildNode(xml, "parity"))
			parity = Base.getChildNodeValue(xml, "parity").charAt(0);
		if (Base.hasChildNode(xml, "databits"))
			databits = Integer.parseInt(Base.getChildNodeValue(xml, "databits"));
		if (Base.hasChildNode(xml, "stopbits"))
			stopbits = Integer.parseInt(Base.getChildNodeValue(xml, "stopbits"));
	}
	
	public void initialize()
	{
		//declare our serial guy.
		try {
			System.out.println("Connecting to " + name + " at " + rate);
			serial = new Serial(name, rate, parity, databits, stopbits);
		} catch (SerialException e) {
			//TODO: report the error here.
			e.printStackTrace();
		}
		
		//wait til we're initialized
		try {
			System.out.println("Initializing Serial.");
			while (!isInitialized)
				readResponse();
		} catch (Exception e) {
			//todo: handle init exceptions here
		}
		
		System.out.println("Ready to rock.");
	}
	
	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void execute()
	{
		// we *DONT* want to use the parents one, 
		// as that will call all sorts of misc functions.
		// we'll simply pass it along.
		//super.execute();
		
		sendCommand(parser.getCommand());
	}
	
	protected void sendCommand(String next)
	{
		next = clean(next);
		
		//skip empty commands.
		if (next.length() == 0)
			return;

		//check to see if we got a response.
		do {
			readResponse();
		}
		while (bufferSize + next.length() + 1 > maxBufferSize);
		//while (bufferSize > 0);
		
		//will it fit into our buffer?
		if (bufferSize + next.length() < maxBufferSize)
		{
			//do the actual send.
			serial.write(next + "\n");
			
			//record it in our buffer tracker.
			commands.add(next);
			bufferSize += next.length() + 1;
			bufferLength++;
			
			//debug... let us know whts up!
			//System.out.println("Sent: " + next);
			//System.out.println("Buffer: " + bufferSize + " (" + bufferLength + " commands)");
		}
		
		//check for quick errors or something.
		readResponse();
	}
	
	public String clean(String str)
	{
		String clean = str;
		
		//trim whitespace
		clean = clean.trim();	
		
		//remove spaces
		clean = clean.replaceAll(" ", "");
		
		return clean;
	}
	
	public void readResponse()
	{
		String cmd = "";
		
		//read for any results.
		for (;;)
		{
			try
			{
				//no serial? bail!
				if (serial.available() > 0)
				{
					//get it as ascii.
					char c = serial.readChar();
					result += c;
				
					//System.out.println("got: " + c);
					//System.out.println("current: " + result);
					
					//is it a done command?
					if (c == '\n')
					{
						//System.out.println("received: " + result);
						
						if (result.startsWith("ok"))
						{
							cmd = (String)commands.get(currentCommand);

							//if (result.length() > 2)
							//	System.out.println("got: " + result.substring(0, result.length()-2) + "(" + bufferSize + " - " + (cmd.length() + 1) + " = " + (bufferSize - (cmd.length() + 1)) + ")");

							bufferSize -= cmd.length() + 1;
							bufferLength--;
							
							currentCommand++;
							result = "";
							
							//Debug.d("Buffer: " + bufferSize + " (" + bufferLength + " commands)");

							//bail, buffer is almost empty.  fill it!
							if (bufferLength < 2)
								break;
							
							//we'll never get here.. for testing.
							//if (bufferLength == 0)
							//	Debug.d("Empty buffer!! :(");
						}
						else if (result.startsWith("T:"))
							System.out.println(result.substring(0, result.length()-2));
						//old arduino firmware sends "start"
						else if (result.startsWith("start"))
						{
							//todo: set version
							isInitialized = true;
						}
						else if (result.startsWith("Extruder Fail"))
						{
							setError("Extruder failed:  cannot extrude as this rate.");
							result = "";
							
							break;
						}
						else
							System.out.println(result.substring(0, result.length()-2));
							
						result = "";
					}
				}
				else
					break;
			} catch (Exception e) {
				break;
			}
		}	
	}
	
	public boolean isFinished()
	{
		try {
			readResponse();
		} catch (Exception e) {
		}
		return (bufferSize == 0);
	}
	
	public void dispose()
	{
		super.dispose();
		
		serial.dispose();
		serial = null;
		commands = null;
	}
	
	/****************************************************
	*  commands for interfacing with the driver directly
	****************************************************/
	
	public void queuePoint(Point3d p)
	{
		String cmd = "G1 X" + p.x + " Y" + p.y + " Z" + p.z + " F" + getCurrentFeedrate();
		
		sendCommand(cmd);
	}
	
	public void homeXYZ()
	{
		sendCommand("G28 XYZ");
	}

	public void homeXY()
	{
		sendCommand("G28 XY");
	}

	public void homeX()
	{
		sendCommand("G28 X");
	}

	public void homeY()
	{
		sendCommand("G28 Y");
	}

	public void homeZ()
	{
		sendCommand("G28 Z");
	}
	
	public void delay(long millis)
	{
		int seconds = Math.round(millis/1000);

		sendCommand("G4 P" + seconds);
	}
	
	public void openClamp(int clampIndex)
	{
		sendCommand("M11 Q" + clampIndex);
	}
	
	public void closeClamp(int clampIndex)
	{
		sendCommand("M10 Q" + clampIndex);
	}
	
	public void enableDrives()
	{
		sendCommand("M17");
	}
	
	public void disableDrives()
	{
		sendCommand("M18");
	}
	
	public void changeGearRatio(int ratioIndex)
	{
		//gear ratio codes are M40-M46
		int code = 40 + ratioIndex;
		code = Math.max(40, code);
		code = Math.min(46, code);
		
		sendCommand("M" + code);
	}
	
	private String _getToolCode()
	{
		return "T" + currentTool().getIndex() + " ";
	}

	/*************************************
	*  Motor interface functions
	*************************************/
	public void setMotorSpeed(double rpm)
	{
		sendCommand(_getToolCode() + "M108 R" + rpm);

		super.setMotorSpeed(rpm);
	}
	
	public void enableMotor()
	{
		String command = _getToolCode();

		if (currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE)
			command += "M101";
		else
			command += "M102";

		sendCommand(command);

		super.enableMotor();
	}
	
	public void disableMotor()
	{
		sendCommand(_getToolCode() + "M103");

		super.disableMotor();
	}

	/*************************************
	*  Spindle interface functions
	*************************************/
	public void setSpindleSpeed(double rpm)
	{
		sendCommand(_getToolCode() + "S" + rpm);

		super.setSpindleSpeed(rpm);
	}
	
	public void enableSpindle()
	{
		String command = _getToolCode();

		if (currentTool().getSpindleDirection() == ToolModel.MOTOR_CLOCKWISE)
			command += "M3";
		else
			command += "M4";

		sendCommand(command);
		
		super.enableSpindle();
	}
	
	public void disableSpindle()
	{
		sendCommand(_getToolCode() + "M5");

		super.disableSpindle();
	}
	
	public void readSpindleSpeed()
	{
		sendCommand(_getToolCode() + "M50");
		
		super.readSpindleSpeed();
	}
	
	/*************************************
	*  Temperature interface functions
	*************************************/
	public void setTemperature(double temperature)
	{
		sendCommand(_getToolCode() + "M104 S" + temperature);
		
		super.setTemperature(temperature);
	}

	public void readTemperature()
	{
		sendCommand(_getToolCode() + "M105");
		
		super.readTemperature();
	}

	/*************************************
	*  Flood Coolant interface functions
	*************************************/
	public void enableFloodCoolant()
	{
		sendCommand(_getToolCode() + "M7");
		
		super.enableFloodCoolant();
	}
	
	public void disableFloodCoolant()
	{
		sendCommand(_getToolCode() + "M9");
		
		super.disableFloodCoolant();
	}

	/*************************************
	*  Mist Coolant interface functions
	*************************************/
	public void enableMistCoolant()
	{
		sendCommand(_getToolCode() + "M8");
		
		super.enableMistCoolant();
	}
	
	public void disableMistCoolant()
	{
		sendCommand(_getToolCode() + "M9");

		super.disableMistCoolant();
	}

	/*************************************
	*  Fan interface functions
	*************************************/
	public void enableFan()
	{
		sendCommand(_getToolCode() + "M106");
		
		super.enableFan();
	}
	
	public void disableFan()
	{
		sendCommand(_getToolCode() + "M107");
		
		super.disableFan();
	}
	
	/*************************************
	*  Valve interface functions
	*************************************/
	public void openValve()
	{
		sendCommand(_getToolCode() + "M126");
		
		super.openValve();
	}
	
	public void closeValve()
	{
		sendCommand(_getToolCode() + "M127");
		
		super.closeValve();
	}
	
	/*************************************
	*  Collet interface functions
	*************************************/
	public void openCollet()
	{
		sendCommand(_getToolCode() + "M21");
		
		super.openCollet();
	}
	
	public void closeCollet()
	{
		sendCommand(_getToolCode() + "M22");
		
		currentTool().closeCollet();
	}
}
