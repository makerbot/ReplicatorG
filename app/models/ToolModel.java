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

package processing.app.models;

import org.w3c.dom.*;


public class ToolModel
{
	public static int MOTOR_CLOCKWISE = 1;
	public static int MOTOR_COUNTER_CLOCKWISE = 2;
	
	//our xml config info
	protected Node xml;
	
	//descriptive stuff
	int index;
	String name;

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

	/*************************************
	*  Creates the model object.
	*************************************/
	public ToolModel(Node n)
	{
		//default information
		index = 0;
		name = "Generic Tool";
		
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

		//load our XML config
		loadXML(n);
	}
	
	//load data from xml config
	public void loadXML(Node node)
	{
		xml = node;
	}
	
	/*************************************
	*  Generic tool information
	*************************************/
	public int getIndex()
	{
		return index;
	}
	public void setIndex(int i)
	{
		index = i;
	}
	
	public String getName()
	{
		return name;
	}
	public void setName(String n)
	{
		name = n;
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
}
