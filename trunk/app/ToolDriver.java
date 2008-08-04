/*
  Driver.java

  Provides an interface for driving various machines.

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

package processing.app;
import processing.app.drivers.*;
//import processing.app.tooldrivers.*;

import org.w3c.dom.*;


public class ToolDriver
{
	public static int MOTOR_FORWARD = 1;
	public static int MOTOR_REVERSE = 2;
	
	//our xml config info
	protected Node xml;
	
	//our driver object
	protected Driver driver;

	/**
	  * Creates the driver object.
	  */
	public ToolDriver(Node n, Driver d)
	{
		driver = d;
		xml = n;
	}
	
	public void setMotorDirection(int dir) {}
	public void setMotorSpeed(double speed) {}
	public void enableMotor() {}
	public void disableMotor() {}
	
	public void setTemperature() {}
	public void readTemperature() {}

	public void enableFan() {}
	public void disableFan() {}
	
	public void openValve() {}
	public void closeValve() {}
}
