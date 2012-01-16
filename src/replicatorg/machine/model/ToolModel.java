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

package replicatorg.machine.model;

import java.util.concurrent.atomic.AtomicReference;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.tools.XML;

public class ToolModel
{
	public static int MOTOR_CLOCKWISE = 1;
	public static int MOTOR_COUNTER_CLOCKWISE = 2;
	
	// TODO: should this be a bitfield?
	protected int toolStatus;
	
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
	protected double motorSpeedRPM;
	protected int motorSpeedPWM;
	protected double motorSpeedReadingRPM;
	protected int motorSpeedReadingPWM;
	protected boolean motorUsesRelay = false;
	protected boolean motorHasEncoder;
	protected int motorEncoderPPR;
	protected boolean motorIsStepper;
	protected double motorSteps; // motor steps per full rotation
	protected String motorStepperAxis;    // Stepper axis this motor is connected to

	//spindle stuff
	protected boolean spindleEnabled;
	protected int spindleDirection;
	protected double spindleSpeedRPM;
	protected int spindleSpeedPWM;
	protected double spindleSpeedReadingRPM;
	protected int spindleSpeedReadingPWM;
	protected boolean spindleHasEncoder;
	protected int spindleEncoderPPR;

	//temperature variables
	protected final AtomicReference<Double> currentTemperature =
		new AtomicReference<Double>(0.0);
	protected double targetTemperature;

	//platform temperature variables
	protected AtomicReference<Double> platformCurrentTemperature = 
		new AtomicReference<Double>(0.0);
	protected double platformTargetTemperature;

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
	protected boolean hasHeatedPlatform = false;
	protected boolean hasAutomatedPlatform = false;
	protected boolean hasFloodCoolant = false;
	protected boolean hasMistCoolant = false;
	protected boolean hasFan = false;
	protected boolean hasValve = false;
	protected boolean hasCollet = false;
	protected boolean alwaysReadHBP = false;

	
	protected boolean automatedBuildPlatformEnabled;

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
	
	/**
	 * Returns true if the parameter is "1" or "true".
	 */
	private boolean isTrueOrOne(String s) {
		if (s == null) {
			return false;
		}
		if (Boolean.parseBoolean(s)) {
			return true; 
		}
		try {
			if (Integer.parseInt(s) == 1) {
				return true;
			}
		} catch (NumberFormatException e) {
		}
		return false;
	}
	
	//load data from xml config
	public void loadXML(Node node)
	{
		xml = node;
		
		//load our name.
		String n = XML.getAttributeValue(xml, "name");
		if (n != null)
			name = n;
		
		//load our index.
		n = XML.getAttributeValue(xml, "index");
		if (n != null)
			index = Integer.parseInt(n);

		//load our type.
		n = XML.getAttributeValue(xml, "type");
		if (n != null)
			type = n;
		
		//load our material
		n = XML.getAttributeValue(xml, "material");
		if (n != null)
			material = n;
		
		n = XML.getAttributeValue(xml, "log_hbp");
		if (n != null && isTrueOrOne(n) )
			alwaysReadHBP = true;

		//our various capabilities
		n = XML.getAttributeValue(xml, "motor");
		if (n != null && isTrueOrOne(n))
		{
			hasMotor = true;

			n = XML.getAttributeValue(xml, "motor_encoder_ppr");
			try{
				if (n != null && Integer.parseInt(n) > 0)
				{
					motorHasEncoder = true;
					motorEncoderPPR = Integer.parseInt(n);
				}
			} catch (Exception e) {} // ignore parse errors.

			// Get number of steps per full rotation of the toolhead motor.
			n = XML.getAttributeValue(xml, "motor_steps");
			try{
				if (n != null && Double.parseDouble(n) > 0.0)
				{
					motorIsStepper = true;
					motorSteps = Double.parseDouble(n);
				}
			} catch (Exception e) {} // ignore parse errors.

			n = XML.getAttributeValue(xml, "stepper_axis");
			try{
				if (n != null && n.length() > 0) {
					motorStepperAxis = n;
				}
			} catch (Exception e) {} // ignore parse errors.
			
			n = XML.getAttributeValue(xml, "default_rpm");
			try{
				if (n != null && Double.parseDouble(n) > 0)
				{
					motorSpeedRPM = Double.parseDouble(n);
				}
			} catch (Exception e) {} // ignore parse errors.

			n = XML.getAttributeValue(xml, "default_pwm");
			try{
				if (n != null && Integer.parseInt(n) > 0)
				{
					motorSpeedPWM = Integer.parseInt(n);
				}
			} catch (Exception e) {} // ignore parse errors.
			
			n = XML.getAttributeValue(xml, "uses_relay");
			try{
				if (n != null)
				{
					motorUsesRelay = isTrueOrOne(n);
					if (motorUsesRelay) {
						Base.logger.severe("Notice: Motor controller configured to use relay, PWM values will be overridden");
					}
				}
			} catch (Exception e) {} // ignore parse errors.
		}

		n = XML.getAttributeValue(xml, "spindle");
		if (isTrueOrOne(n))
		{
			hasSpindle = true;

			n = XML.getAttributeValue(xml, "motor_encoder_ppr");
			try{
				if (n != null && Integer.parseInt(n) > 0)
				{
					motorHasEncoder = true;
					motorEncoderPPR = Integer.parseInt(n);
				}
			} catch (Exception e) {} // ignore parse errors.
		}

		//flood coolant
		n = XML.getAttributeValue(xml, "floodcoolant");
		hasFloodCoolant = hasFloodCoolant || isTrueOrOne(n);

		n = XML.getAttributeValue(xml, "mistcoolant");
		hasMistCoolant = hasMistCoolant || isTrueOrOne(n);

		n = XML.getAttributeValue(xml, "fan");
		hasFan = hasFan || isTrueOrOne(n);

		n = XML.getAttributeValue(xml, "valve");
		hasValve = hasValve || isTrueOrOne(n);

		n = XML.getAttributeValue(xml, "collet");
		hasCollet = hasCollet || isTrueOrOne(n);

		n = XML.getAttributeValue(xml, "heater");
		hasHeater = hasHeater || isTrueOrOne(n);

		n = XML.getAttributeValue(xml, "heatedplatform");
		hasHeatedPlatform = hasHeatedPlatform || isTrueOrOne(n);

		n = XML.getAttributeValue(xml, "automatedplatform");
		hasAutomatedPlatform = hasAutomatedPlatform || isTrueOrOne(n);

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
		if (hasHeatedPlatform)
			result += "hasHeatedPlatform, ";
		if (hasAutomatedPlatform)
			result += "hasAutomatedPlatform, ";
		if (motorIsStepper) {
			result += "motorIsStepper, ";
			result += "motorStepperAxis: " + motorStepperAxis + ", ";
			result += "motorSteps: " + motorSteps + ", ";
		}
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
	
	public int getToolStatus()
	{
		return toolStatus;
	}

	public void setToolStatus(int status)
	{
		toolStatus = status;
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
	
	public void setMotorSpeedRPM(double rpm)
	{
		motorSpeedRPM = rpm;
	}

	public void setMotorSpeedPWM(int pwm)
	{
		motorSpeedPWM = pwm;
	}
	
	public double getMotorSpeedRPM()
	{
		return motorSpeedRPM;
	}
	
	/**
	 * Get number of steps per revolution
	 */
	public double getMotorSteps()
	{
		return motorSteps;
	}

	public int getMotorSpeedPWM()
	{
		return motorSpeedPWM;
	}
	
	public boolean getMotorUsesRelay()
	{
		return motorUsesRelay;
	}
	
	public void setMotorSpeedReadingRPM(double rpm)
	{
		motorSpeedReadingRPM = rpm;
	}
	
	public void setMotorSpeedReadingPWM(int pwm)
	{
		motorSpeedReadingPWM = pwm;
	}
	
	public double getMotorSpeedReadingRPM()
	{
		return motorSpeedReadingRPM;
	}

	public int getMotorSpeedReadingPWM()
	{
		return motorSpeedReadingPWM;
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
	
	public boolean motorHasEncoder()
	{
	  return motorHasEncoder;
	}
	
	public boolean motorIsStepper()
	{
		return motorIsStepper;
	}
	
	/**
	 * 
	 * @return null if motorstepperaxis wasn't specified. The axis identifier otherwise
	 */
	public String getMotorStepperAxis()
	{
		return motorStepperAxis;
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
	
	public void setSpindleSpeedRPM(double rpm)
	{
		spindleSpeedRPM = rpm;
	}

	public void setSpindleSpeedPWM(int pwm)
	{
		spindleSpeedPWM = pwm;
	}
	
	public double getSpindleSpeedRPM()
	{
		return spindleSpeedRPM;
	}

	public int getSpindleSpeedPWM()
	{
		return spindleSpeedPWM;
	}
	
	public void setSpindleSpeedReadingRPM(double rpm)
	{
		spindleSpeedReadingRPM = rpm;
	}
	
	public void setSpindleSpeedReadingPWM(int pwm)
	{
		spindleSpeedReadingPWM = pwm;
	}
	
	public double getSpindleSpeedReadingRPM()
	{
		return spindleSpeedReadingRPM;
	}

	public int getSpindleSpeedReadingPWM()
	{
		return spindleSpeedReadingPWM;
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
	
	public boolean spindleHasEncoder()
	{
	  return spindleHasEncoder;
	}

	/*************************************
	*  Heater interface functions
	*************************************/
	public void setTargetTemperature(double temperature)
	{
		targetTemperature = temperature;
	}

	public double getTargetTemperature()
	{
		return targetTemperature;
	}

	public void setCurrentTemperature(double temperature)
	{
		currentTemperature.set( temperature );
	}
	
	public double getCurrentTemperature()
	{
		return currentTemperature.get();
	}
	
	public boolean hasHeater()
	{
		return hasHeater;
	}

	/*************************************
	*  Heated Platform interface functions
	*************************************/
	public void setPlatformTargetTemperature(double temperature)
	{
		platformTargetTemperature = temperature;
	}

	public double getPlatformTargetTemperature()
	{
		return platformTargetTemperature;
	}

	public void setPlatformCurrentTemperature(double temperature)
	{
		platformCurrentTemperature.set(temperature);
	}
	
	public double getPlatformCurrentTemperature()
	{
		return platformCurrentTemperature.get();
	}
	
	public boolean hasHeatedPlatform()
	{
		return hasHeatedPlatform;
	}

	public boolean hasAutomatedPlatform()
	{
		return hasAutomatedPlatform;
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
	{ return hasMistCoolant; }

	public void setAutomatedBuildPlatformRunning(boolean state) {
		automatedBuildPlatformEnabled = state;
	}
	public boolean isAutomatedBuildPlatformEnabled(boolean state) {
		return automatedBuildPlatformEnabled;
	}
	
	/*************************************
	*  Fan interface functions
	*************************************/
	public void enableFan() { fanEnabled = true; }

	public void disableFan() { fanEnabled = false; }
	
	public boolean isFanEnabled() { return fanEnabled; }
	
	public boolean hasFan() { return hasFan; }
	
	public boolean alwaysReadBuildPlatformTemp() { return  alwaysReadHBP; }

	/*************************************
	*  Valve interface functions
	*************************************/
	public void openValve() { valveOpen = true; }
	
	public void closeValve() { valveOpen = false; }
	
	public boolean isValveOpen() {return valveOpen;	}
	
	public boolean hasValve() { return hasValve; }
	
	/*************************************
	*  Collet interface functions
	*************************************/
	public void openCollet() { colletOpen = true; }
	
	public void closeCollet() { colletOpen = false;	}
	
	public boolean isColletOpen() {	return colletOpen; }
	
	public boolean hasCollet() { return hasCollet;}
	
	/**
	 * Retrieve XML node. A temporary hack until new  tool models.
	 */
	public Node getXml() { return xml; }
}
