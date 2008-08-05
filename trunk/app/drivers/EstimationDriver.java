/*
  EstimationDriver.java

  This driver estimates the build time, etc.

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
import processing.core.*;
import org.w3c.dom.*;
import javax.vecmath.*;


public class EstimationDriver extends Driver
{
	//build time in milliseconds
	private long buildTime;
	
	public EstimationDriver()
	{
		super();
	}
	
	public void delay(long millis)
	{
		buildTime += millis;
	}
	
	public void execute()
	{
		//suppress errors.
		try {
			super.execute();
		} catch (GCodeException e) {}
	}
	
	public void queuePoint(Point3d p)
	{
		Point3d current = getCurrentPosition();
		
		//calculate the length of each axis move
		double xFactor = Math.pow(p.x - current.x, 2);
		double yFactor = Math.pow(p.y - current.y, 2);
		double zFactor = Math.pow(p.z - current.z, 2);

		//get the total length
		double length = Math.sqrt(xFactor + yFactor + zFactor);

		// our speed is feedrate * distance * 60000 (milliseconds in 1 minute)
		// feedrate is mm per minute
		double millis = length / getCurrentFeedrate() * 60000.0;

		//debug.
		//System.out.println(getCurrentPosition().toString() + " to " + p.toString() + " (" + length + "mm) at " + getCurrentFeedrate() + " takes " + Math.round(millis) + " millis.");
		
		//add it in!
		buildTime += millis;
		
		//send it off!
		super.queuePoint(p);
	}
	
	public long getBuildTime()
	{
		return buildTime;
	}
	
	public String getBuildTimeString()
	{
		//System.out.println("build millis = " + getBuildTime());
		
		String val = new String();
		long tempTime = getBuildTime();
		
		//figure out days
		int days = (int)Math.floor(tempTime / 86400000.0);
		if (days > 0)
		{
			tempTime = tempTime - (days * 86400000); 

			//string formatting
			val += days + " day";
			if (days > 1)
				val += "s";
		}

		//figure out hours
		int hours = (int)Math.floor(tempTime / 3600000.0); 
		if (hours > 0)
		{
			tempTime = tempTime - (hours * 3600000);

			//string formatting
			if (days > 0)
				val += ", ";
			val += hours + " hour";
			if (hours > 1)
				val += "s";
		}
		
		//figure out minutes
		int minutes = (int)Math.floor(tempTime / 60000.0);
		if (minutes > 0)
		{
			tempTime = tempTime - (minutes * 60000);

			//string formatting
			if (days > 0 || hours > 0)
				val += ", ";
			val += minutes + " minute";
			if (minutes > 1)
				val += "s";
		}
		
		return val;
	}
}
