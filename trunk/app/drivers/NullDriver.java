/*
  NullDriver.java

  This driver does absolutely nothing.

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


public class NullDriver extends Driver
{
	private int delay;
	
	public NullDriver()
	{
		super();

		delay = 100;
	}
	
	public NullDriver(int d)
	{
		super();

		delay = d;
	}
	
	public NullDriver(Node node)
	{
		super();
		delay = 100;
	}
	
	public void execute()
	{
		super.execute();
		
		if (delay > 0)
		{
			try {
				Thread.currentThread().sleep(delay);
			} catch (InterruptedException e) {}
		}
	}
	
	public void queuePoint(Point3d p)
	{
		current = p;
	}
	
}
