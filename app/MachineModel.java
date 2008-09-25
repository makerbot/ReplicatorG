/*
  MachineModel.java

  A class to store and model a 3-axis machine.

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

import org.w3c.dom.*;
import javax.vecmath.*;

public class MachineModel
{
	//our xml config info
	protected Node xml;
	
	//our machine space
	private Point3d currentPosition;
	private Point3d minimum;
	private Point3d maximum;

	//feedrate information
	private Point3d maximumFeedrates;
	private Point3d stepsPerMM;

	/*************************************
	*  Creates the model object.
	*************************************/
	public MachineModel()
	{
		currentPosition = new Point3d();
		minimum = new Point3d();
		maximum = new Point3d();
		maximumFeedrates = new Point3d();
		stepsPerMM = new Point3d(1, 1, 1); //use ones, because we divide by this!
	}
	
	//load data from xml config
	public void loadXML(Node node)
	{
		xml = node;
	}

	/*************************************
	* Basic positioning information
	*************************************/
	public Point3d getCurrentPosition()
	{
		return new Point3d(currentPosition);
	}
	
	public void setCurrentPosition(Point3d p)
	{
		currentPosition = new Point3d(p);
	}

	/*************************************
	*  Convert steps to millimeter units
	*************************************/
	public double xStepsToMM(long steps)
	{
		return steps/stepsPerMM.x;
	}
	
	public double yStepsToMM(long steps)
	{
		return steps/stepsPerMM.y;
	}
	
	public double zStepsToMM(long steps)
	{
		return steps/stepsPerMM.z;
	}
	
	public Point3d stepsToMM(Point3d steps)
	{
		Point3d temp = new Point3d();

		temp.x = steps.x/stepsPerMM.x;
		temp.y = steps.y/stepsPerMM.y;
		temp.z = steps.z/stepsPerMM.z;
		
		return temp;
	}
	
	/*************************************
	*  Convert millimeters to machine steps
	*************************************/
	public long xMMtoSteps(double mm)
	{
		return Math.round(mm * stepsPerMM.x);
	}
	
	public long yMMtoSteps(double mm)
	{
		return Math.round(mm * stepsPerMM.y);
	}
	
	public long zMMtoSteps(double mm)
	{
		return Math.round(mm * stepsPerMM.z);
	}

	public Point3d mmToSteps(Point3d mm)
	{
		Point3d temp = new Point3d();

		temp.x = Math.round(mm.x * stepsPerMM.x);
		temp.y = Math.round(mm.y * stepsPerMM.y);
		temp.z = Math.round(mm.z * stepsPerMM.z);
		
		return temp;
	}
}
