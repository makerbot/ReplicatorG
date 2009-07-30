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

package replicatorg.drivers;

import java.awt.geom.Rectangle2D;

import replicatorg.app.exceptions.GCodeException;
import javax.vecmath.Point3d;

public class EstimationDriver extends DriverBaseImplementation {
	// build time in milliseconds
	private double buildTime = 0.0;

	private Rectangle2D.Double bounds = new Rectangle2D.Double();
	
	public EstimationDriver() {
		super();

		buildTime = 0.0;
	}

	public Rectangle2D.Double getBounds() { return bounds; }
	
	public void delay(long millis) {
		buildTime += (double) millis / 1000;
	}

	public void execute() throws InterruptedException {
		// suppress errors.
		try {
			super.execute();
		} catch (GCodeException e) {
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	protected void queuePoint(Point3d p, Double feedrate) {
		// our speed is feedrate * distance * 60000 (milliseconds in 1 minute)
		// feedrate is mm per minute
		double millis = getMoveLength() / feedrate * 60000.0;

		bounds.add(p.x,p.y);
		
		// add it in!
		if (millis > 0) {
			buildTime = buildTime + millis;
			// System.out.println(getMoveLength() + "mm at " + feedrate + "
			// takes " + Math.round(millis) + " millis (" + buildTime + "
			// total).");
		}
	}

	public double getBuildTime() {
		return buildTime;
	}

	static public String getBuildTimeString(double tempTime) {
		return getBuildTimeString(tempTime, false);
	}

	static public String getBuildTimeString(double tempTime, boolean useSeconds) {
		// System.out.println("build millis = " + tempTime);

		String val = new String();

		// figure out days
		int days = (int) Math.floor(tempTime / 86400000.0);
		if (days > 0) {
			tempTime = tempTime - (days * 86400000);

			// string formatting
			val += days + " day";
			if (days > 1)
				val += "s";
		}

		// figure out hours
		int hours = (int) Math.floor(tempTime / 3600000.0);
		if (hours > 0) {
			tempTime = tempTime - (hours * 3600000);

			// string formatting
			if (days > 0)
				val += ", ";
			val += hours + " hour";
			if (hours > 1)
				val += "s";
		}

		// figure out minutes
		int minutes = (int) Math.floor(tempTime / 60000.0);
		if (!useSeconds)
			minutes++; // lets just round up the remainder...
		if (minutes > 0) {
			tempTime = tempTime - (minutes * 60000);

			// string formatting
			if (days > 0 || hours > 0)
				val += ", ";
			val += minutes + " minute";
			if (minutes > 1)
				val += "s";
		}

		// figure out minutes
		if (useSeconds) {
			int seconds = (int) Math.floor(tempTime / 1000.0);
			if (seconds > 0) {
				tempTime = tempTime - (seconds * 1000);

				// string formatting
				if (days > 0 || hours > 0 || minutes > 0)
					val += ", ";
				val += seconds + " second";
				if (seconds > 1)
					val += "s";
			}
		}

		return val;
	}
}
