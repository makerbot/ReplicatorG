/*
  ToolModel.java

  A class to model a toolhead.

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

package replicatorg.app.models;

import org.w3c.dom.Node;

import replicatorg.app.tools.XML;

public class ToolModel
{
	public static int MOTOR_CLOCKWISE = 1;
	public static int MOTOR_COUNTER_CLOCKWISE = 2;
	
	//our xml config info
	protected Node xml;
	
	//descriptive stuff
	protected String name;
	protected String type;
	protected String material;
	protected int index;

	//motor stuff
	protected boolean motorEnabled;
	protected int motorDirection;
	protected double motorSpeed;

	//spindle stuff
	protected boolean spindleEnabled;
	protected int spindleDirection;
	protected double spindleSpeed;

	//temperature variables
	protected double currentTemperature;
	protected double targetTemperature;

	//various coolant/control stuff
	protected boolean floodCoolantEnabled;
	protected boolean mistCoolantEnabled;
	protected boolean fanEnabled;
	protected boolean valveOpen;
	protected boolean colletOpen;
	
	//capabilities
	protected boolean hasMotor = false;
	protected boolean hasSpindle = false;
	protected boolean hasHeater = false;
	protected boolean hasFloodCoolant = false;
	protected boolean hasMistCoolant = false;
	protected boolean hasFan = false;
	protected boolean hasValve = false;
	protected boolean hasCollet = false;

	/*************************************
	*  Creates the model object.
	*************************************/
	public ToolModel()
	{
		_initialize();
	}
	
	public ToolModel(Node n)
	{
		_initialize();

		//load our XML config
		loadXML(n);
	}
	
	private void _initialize()
	{
		//default information
		name = "Generic Tool";
		type = "tool";
		material = "unknown";
		index = 0;
		
		//default our spindles/motors
		setMotorDirection(MOTOR_CLOCKWISE);
		disableMotor();
		setSpindleDirection(MOTOR_CLOCKWISE);
		disableMotor();
		
		//default our accessories
		disableFloodCoolant();
		disableMistCoolant();
		disableFan();
		closeValve();
		closeCollet();
	}
	
	//load data from xml config
	public void loadXML(Node node)
	{
		xml = node;
		
		//load our name.
		String n = XML.getAttributeValue(xml, "name");
		if (n != null)
			name = n;
			
		//load our type.
		n = XML.getAttributeValue(xml, "type");
		if (n != null)
			type = n;
		
		//load our material
		n = XML.getAttributeValue(xml, "material");
		if (n != null)
			material = n;
		
		//our various capabilities
		n = XML.getAttributeValue(xml, "motor");
		try {
			if (Boolean.parseBoolean(n) || Integer.parseInt(n) == 1)
				hasMotor = true;
		} catch (Exception e) {} //ignore boolean/integer parse errors

		n = XML.getAttributeValue(xml, "spindle");
		try {
			if (Boolean.parseBoolean(n) || Integer.parseInt(n) == 1)
				hasSpindle = true;
		} catch (Exception e) {} //ignore boolean/integer parse errors

		//flood coolant
		n = XML.getAttributeValue(xml, "floodcoolant");
		try {
			if (Boolean.parseBoolean(n) || Integer.parseInt(n) == 1)
				hasFloodCoolant = true;
		} catch (Exception e) {} //ignore boolean/integer parse errors

		n = XML.getAttributeValue(xml, "mistcoolant");
		try {
			if (Boolean.parseBoolean(n) || Integer.parseInt(n) == 1)
				hasMistCoolant = true;
		} catch (Exception e) {} //ignore boolean/integer parse errors

		n = XML.getAttributeValue(xml, "fan");
		try {
			if (Boolean.parseBoolean(n) || Integer.parseInt(n) == 1)
				hasFan = true;
		} catch (Exception e) {} //ignore boolean/integer parse errors

		n = XML.getAttributeValue(xml, "valve");
		try {
			if (Boolean.parseBoolean(n) || Integer.parseInt(n) == 1)
				hasValve = true;
		} catch (Exception e) {} //ignore boolean/integer parse errors

		n = XML.getAttributeValue(xml, "collet");
		try {
			if (Boolean.parseBoolean(n) || Integer.parseInt(n) == 1)
				hasCollet = true;
		} catch (Exception e) {} //ignore boolean/integer parse errors

		n = XML.getAttributeValue(xml, "heater");
		try {
			if (Boolean.parseBoolean(n) || Integer.parseInt(n) == 1)
				hasHeater = true;
		} catch (Exception e) {} //ignore boolean/integer parse errors

		//hah, all this for a debug string... lol.
		String result = "Loading " + type + " '" + name + "': ";
		result += "material: " + material + ", ";
		result += "with these capabilities: ";
		if (hasFloodCoolant)
			result += "flood coolant, ";
		if (hasMotor)
			result += "motor, ";
		if (hasSpindle)
			result += "spindle, ";
		if (hasMistCoolant)
			result += "mist coolant, ";
		if (hasFan)
			result += "fan, ";
		if (hasValve)
			result += "valve, ";
		if (hasCollet)
			result += "collet, ";
		if (hasHeater)
			result += "heater, ";
		result += "etc.";
		System.out.println(result);
	}
	
	/*************************************
	*  Generic tool information
	*************************************/
	
	public String getName()
	{
		return name;
	}

	public void setIndex(int i)
	{
		index = i;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public String getType()
	{
		return type;
	}

	/*************************************
	*  Motor interface functions
	*************************************/
	public void setMotorDirection(int dir)
	{
		motorDirection = dir;
	}

	public int getMotorDirection()
	{
		return motorDirection;
	}
	
	public void setMotorSpeed(double rpm)
	{
		motorSpeed = rpm;
	}
	
	public double getMotorSpeed()
	{
		return motorSpeed;
	}
	
	public void enableMotor()
	{
		motorEnabled = true;
	}
	
	public void disableMotor()
	{
		motorEnabled = false;
	}
	
	public boolean isMotorEnabled()
	{
		return motorEnabled;
	}
	
	public boolean hasMotor()
	{
		return hasMotor;
	}
	
	/*************************************
	*  Spindle interface functions
	*************************************/
	public void setSpindleDirection(int dir)
	{
		spindleDirection = dir;
	}

	public int getSpindleDirection()
	{
		return spindleDirection;
	}
	
	public void setSpindleSpeed(double rpm)
	{
		spindleSpeed = rpm;
	}
	
	public double getSpindleSpeed()
	{
		return spindleSpeed;
	}
	
	public void enableSpindle()
	{
		spindleEnabled = true;
	}
	
	public void disableSpindle()
	{
		spindleEnabled = false;
	}
	
	public boolean isSpindleEnabled()
	{
		return spindleEnabled;
	}
	
	public boolean hasSpindle()
	{
		return hasSpindle;
	}

	/*************************************
	*  Spindle interface functions
	*************************************/
	public void setTargetTemperature(double temperature)
	{
		targetTemperature = temperature;
	}

	public void setCurrentTemperature(double temperature)
	{
		currentTemperature = temperature;
	}
	
	public double getCurrentTemperature()
	{
		return currentTemperature;
	}
	
	public boolean hasHeater()
	{
		return hasHeater;
	}

	/*************************************
	*  Flood Coolant interface functions
	*************************************/
	public void enableFloodCoolant()
	{
		floodCoolantEnabled = true;
	}
	
	public void disableFloodCoolant()
	{
		floodCoolantEnabled = false;
	}
	
	public boolean isFloodCoolantEnabled()
	{
		return floodCoolantEnabled;
	}
	
	public boolean hasFloodCoolant()
	{
		return hasFloodCoolant;
	}

	/*************************************
	*  Mist Coolant interface functions
	*************************************/
	public void enableMistCoolant()
	{
		mistCoolantEnabled = true;
	}
	
	public void disableMistCoolant()
	{
		mistCoolantEnabled = false;
	}
	
	public boolean isMistCoolantEnabled()
	{
		return mistCoolantEnabled;
	}
	
	public boolean hasMistCoolant()
	{
		return hasMistCoolant;
	}

	/*************************************
	*  Fan interface functions
	*************************************/
	public void enableFan()
	{
		fanEnabled = true;
	}

	public void disableFan()
	{
		fanEnabled = false;
	}
	
	public boolean isFanEnabled()
	{
		return fanEnabled;
	}
	
	public boolean hasFan()
	{
		return hasFan;
	}
	
	/*************************************
	*  Valve interface functions
	*************************************/
	public void openValve()
	{
		valveOpen = true;
	}
	
	public void closeValve()
	{
		valveOpen = false;
	}
	
	public boolean isValveOpen()
	{
		return valveOpen;
	}
	
	public boolean hasValve()
	{
		return hasValve;
	}
	
	/*************************************
	*  Collet interface functions
	*************************************/
	public void openCollet()
	{
		colletOpen = true;
	}
	
	public void closeCollet()
	{
		colletOpen = false;
	}
	
	public boolean isColletOpen()
	{
		return colletOpen;
	}
	
	public boolean hasCollet()
	{
		return hasCollet;
	}
}
