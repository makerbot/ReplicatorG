/*
  ClampModel.java

  A class to model a clamp on a 3-axis machine.

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

import processing.app.tools.*;

import org.w3c.dom.*;

public class ClampModel
{
	//our xml config info
	protected Node xml;
	
	//static vars for state.
	protected static boolean OPEN = true;
	protected static boolean CLOSED = false;
	
	//our state
	protected boolean state;

	/*************************************
	*  Creates the model object.
	*************************************/
	public ClampModel(Node n)
	{
		state = OPEN;
		
		loadXML(n);
	}
	
	//load data from xml config
	public void loadXML(Node node)
	{
		xml = node;
	}

	public boolean getState()
	{
		return state;
	}
	
	public void open()
	{
		setState(OPEN);
	}
	
	public void close()
	{
		setState(CLOSED);
	}
	
	public void setState(boolean status)
	{
		state = status;
	}
	
}
